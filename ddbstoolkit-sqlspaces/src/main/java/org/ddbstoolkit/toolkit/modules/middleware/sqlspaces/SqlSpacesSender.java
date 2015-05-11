package org.ddbstoolkit.toolkit.modules.middleware.sqlspaces;

import info.collide.sqlspaces.client.TupleSpace;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleID;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ddbstoolkit.toolkit.core.DDBSCommand;
import org.ddbstoolkit.toolkit.core.DistributableSenderInterface;
import org.ddbstoolkit.toolkit.core.DistributedEntity;
import org.ddbstoolkit.toolkit.core.IEntity;
import org.ddbstoolkit.toolkit.core.ObjectComparator;
import org.ddbstoolkit.toolkit.core.Peer;
import org.ddbstoolkit.toolkit.core.exception.DDBSToolkitException;
import org.ddbstoolkit.toolkit.core.reflexion.DDBSEntity;
import org.ddbstoolkit.toolkit.core.reflexion.DDBSEntityIDProperty;

/**
 * Class to send commands using SQLSpaces
 * User: Cyril GRANDJEAN
 * Date: 21/06/2012
 * Time: 11:39
 *
 * @version Creation of the class
 */
public class SqlSpacesSender implements DistributableSenderInterface {

    /**
     * Peer corresponding to the interface
     */
    private Peer myPeer;

    /**
     * Indicate if the connection is open
     */
    private boolean isOpen = false;

    /**
     * Maximum timeout
     */
    private int timeout = 10000;

    /**
     * Name of the cluster
     */
    private String clusterName;

    /**
     * TupleSpace for peers
     */
    TupleSpace spacePeers;

    /**
     * TupleSpace for commands
     */
    TupleSpace commandPeers;

    /**
     * Ip address of the server
     */
    String ipAddressServer;

    /**
     * Port of the server
     */
    private int port;

    /**
     * Create a SqlSpaces Sender using localhost server
     * @param clusterName Name of the cluster
     * @param peerName Name of the peer
     */
    public SqlSpacesSender(String clusterName, String peerName) {
        super();
        this.clusterName = clusterName;

        this.ipAddressServer = "127.0.0.1";
        this.port = 2525;
        this.myPeer = new Peer();
        this.myPeer.setName(peerName);
    }

    /**
     * Create a SqlSpaces Sender using an external server
     * @param clusterName Name of the cluster
     * @param peerName Name of the peer
     * @param ipAddress Ip address of the SQLSpaces Server
     * @param port Port of the SQLSpaces Server
     */
    public SqlSpacesSender(String clusterName, String peerName, String ipAddress, int port) {
        this.clusterName = clusterName;
        this.port = port;
        this.ipAddressServer = ipAddress;
        this.myPeer = new Peer();
        this.myPeer.setName(peerName);
    }

    @Override
    public void setPeer(Peer myPeer) {
        this.myPeer = myPeer;
    }

    @Override
    public Peer getPeer() {
        return myPeer;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void open() throws DDBSToolkitException {

        //Get the peers space
        try {
			spacePeers = new TupleSpace(ipAddressServer, port, clusterName+"-peers");
			
			commandPeers = new TupleSpace(ipAddressServer, port, clusterName+"-commands");

	        isOpen = true;
		} catch (TupleSpaceException e) {
			throw new DDBSToolkitException("Error executing the middleware request", e);
		}  
    }

    @Override
    public void close() throws DDBSToolkitException{

    	try
    	{
    		spacePeers.disconnect();

            commandPeers.disconnect();

            isOpen = false;
    	}
    	catch (TupleSpaceException e) {
			throw new DDBSToolkitException("Error executing the middleware request", e);
		} 
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T extends IEntity> ArrayList<T> listAll(T object, List<String> conditionList, String orderBy) throws DDBSToolkitException {

    	try
    	{
	        //Connection must be established
	        if(isOpen == true && object != null)
	        {
	            DistributedEntity myEntity = (DistributedEntity) object;
	
	            DDBSCommand command = new DDBSCommand();
	            command.setAction(DDBSCommand.LIST_ALL_COMMAND);
	            command.setConditionList(conditionList);
	
	            if(myEntity.peerUid != null && !myEntity.peerUid.isEmpty())
	            {
	                command.setDestination(myEntity.peerUid);
	            }
	            else
	            {
	                command.setDestination(DDBSCommand.DESTINATION_ALL_PEERS);
	            }
	            command.setOrderBy(orderBy);
	            command.setObject(object);
	
	            //Get the number of peers
	            int numberOfPeers = getListPeers().size();
	
	            TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));
	
	            //Space to receive ACK
	            TupleSpace ackSpace = new TupleSpace(ipAddressServer, port, clusterName+"-ack-"+id);
	
	            Tuple template = new Tuple(String.class);
	
	            long endTime = System.currentTimeMillis() + timeout;
	
	            //Wait for the answers
	            while((endTime - System.currentTimeMillis() > 0) && numberOfPeers > 0)
	            {
	                ackSpace.waitToTake(template,(endTime - System.currentTimeMillis()));
	
	                numberOfPeers--;
	            }
	
	            ackSpace.disconnect();
	
	            TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
	
	            Tuple[] list = resultSpace.takeAll(template);
	
	            ArrayList<T> returnList = new ArrayList<T>();
	
	            for(int i = 0; i < list.length; i++)
	            {
	                Tuple myTuple = list[i];
	                String encodedValue = (String) myTuple.getField(0).getValue();
	                returnList.add((T) SqlSpacesConverter.fromString(encodedValue));
	            }
	
	            if((myEntity.peerUid == null || myEntity.peerUid.isEmpty()) && orderBy != null && !orderBy.equals(""))
	            {
	                Collections.sort(returnList, new ObjectComparator(orderBy));
	            }
	
	            resultSpace.disconnect();
	
	            return returnList;
	        }
	        else
	        {
	            return null;
	        }
    	}
    	catch (TupleSpaceException e) {
			throw new DDBSToolkitException("Error executing the middleware request", e);
		} 
    	catch (Exception e) {
			throw new DDBSToolkitException("Error executing the middleware request", e);
		} 
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T extends IEntity> T read(T object) throws DDBSToolkitException {

    	try
    	{
    		DistributedEntity myDistributedEntity = (DistributedEntity) object;

            if(isOpen == true && object != null && myDistributedEntity.peerUid != null)
            {

                DDBSCommand command = new DDBSCommand();
                command.setAction(DDBSCommand.READ_COMMAND);
                command.setDestination(myDistributedEntity.peerUid);
                command.setObject(object);

                TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                Tuple template = new Tuple(String.class);
                Tuple result = resultSpace.waitToTake(template);

                resultSpace.disconnect();

                return (T) SqlSpacesConverter.fromString((String) result.getField(0).getValue());
            }
            else
            {
                return null;
            }
    	}
    	catch (TupleSpaceException e) {
			throw new DDBSToolkitException("Error executing the middleware request", e);
		} catch (IOException e) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", e);
		} catch (ClassNotFoundException e) {
			throw new DDBSToolkitException("Class not found exception", e);
		} catch (Exception e) {
			throw new DDBSToolkitException("Error executing the middleware request", e);
		} 
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T extends IEntity> T readLastElement(T object) throws DDBSToolkitException {

    	try
    	{
    		DistributedEntity myDistributedEntity = (DistributedEntity) object;

            if(isOpen == true && object != null && myDistributedEntity.peerUid != null)
            {

                DDBSCommand command = new DDBSCommand();
                command.setAction(DDBSCommand.READ_LAST_ELEMENT_COMMAND);
                command.setDestination(myDistributedEntity.peerUid);
                command.setObject(object);

                TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                Tuple template = new Tuple(String.class);
                Tuple result = resultSpace.waitToTake(template);

                resultSpace.disconnect();

                return (T) SqlSpacesConverter.fromString((String) result.getField(0).getValue());
            }
            else
            {
                return null;
            }
    	}
    	catch (TupleSpaceException tse) {
			throw new DDBSToolkitException("Error executing the middleware request", tse);
		} catch (IOException ioe) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new DDBSToolkitException("Class not found exception", cnfe);
		}
    	catch (Exception e) {
    		throw new DDBSToolkitException("Error executing the middleware request", e);
    	}
    }

    @Override
    public boolean add(IEntity objectToAdd) throws DDBSToolkitException {

    	try
    	{
    		DistributedEntity myDistributedEntity = (DistributedEntity) objectToAdd;

            if(isOpen == true && objectToAdd != null && myDistributedEntity.peerUid != null)
            {

                DDBSCommand command = new DDBSCommand();
                command.setAction(DDBSCommand.ADD_COMMAND);
                command.setDestination(myDistributedEntity.peerUid);
                command.setObject(myDistributedEntity);

                TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                Tuple template = new Tuple(Boolean.class);
                Tuple result = resultSpace.waitToTake(template);

                resultSpace.disconnect();

                return (Boolean) result.getField(0).getValue();
            }
            else
            {
                return false;
            }
    	}
    	catch (TupleSpaceException tse) {
			throw new DDBSToolkitException("Error executing the middleware request", tse);
		} catch (IOException ioe) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new DDBSToolkitException("Class not found exception", cnfe);
		}
    	catch (Exception e) {
    		throw new DDBSToolkitException("Error executing the middleware request", e);
    	}
    }

    @Override
    public boolean update(IEntity objectToUpdate) throws DDBSToolkitException {

    	try
    	{
    		DDBSEntity ddbsEntity = DDBSEntity.getDDBSEntity(objectToUpdate);
    		
    		DistributedEntity myDistributedEntity = (DistributedEntity) objectToUpdate;

            //Connection must be established
            if(isOpen == true && objectToUpdate != null && myDistributedEntity.peerUid != null)
            {

                //List of primary keys
            	List<DDBSEntityIDProperty> listPrimaryKeys = ddbsEntity.getEntityIDProperties();

                DistributedEntity myEntity = (DistributedEntity) objectToUpdate;

                if(listPrimaryKeys.isEmpty() || myEntity.peerUid == null)
                {
                    return false;
                }
                else
                {

                    DDBSCommand command = new DDBSCommand();
                    command.setAction(DDBSCommand.UPDATE_COMMAND);
                    command.setDestination(myDistributedEntity.peerUid);
                    command.setObject(myDistributedEntity);

                    TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                    TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                    Tuple template = new Tuple(Boolean.class);
                    Tuple result = resultSpace.waitToTake(template);

                    resultSpace.disconnect();

                    return (Boolean) result.getField(0).getValue();
                }
            }
            else
            {
                return false;
            }
    	}
    	catch (TupleSpaceException tse) {
			throw new DDBSToolkitException("Error executing the middleware request", tse);
		} catch (IOException ioe) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new DDBSToolkitException("Class not found exception", cnfe);
		}
    	catch (Exception e) {
    		throw new DDBSToolkitException("Error executing the middleware request", e);
    	}
    }

    @Override
    public boolean delete(IEntity objectToDelete) throws DDBSToolkitException {

    	try
    	{
    		DistributedEntity myDistributedEntity = (DistributedEntity) objectToDelete;
    		
    		DDBSEntity ddbsEntity = DDBSEntity.getDDBSEntity(objectToDelete);

            //Connection must be established
            if(isOpen == true && objectToDelete != null && myDistributedEntity.peerUid != null)
            {

                //Check the primary key
                List<DDBSEntityIDProperty> listPrimaryKeys = ddbsEntity.getEntityIDProperties();

                DistributedEntity myEntity = (DistributedEntity) objectToDelete;

                if(listPrimaryKeys.isEmpty() || myEntity.peerUid == null)
                {
                    return false;
                }
                else
                {

                    DDBSCommand command = new DDBSCommand();
                    command.setAction(DDBSCommand.DELETE_COMMAND);
                    command.setDestination(myDistributedEntity.peerUid);
                    command.setObject(myDistributedEntity);

                    TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                    TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                    Tuple template = new Tuple(Boolean.class);
                    Tuple result = resultSpace.waitToTake(template);

                    resultSpace.disconnect();

                    return (Boolean) result.getField(0).getValue();
                }
            }
            else
            {
                return false;
            }
    	}
    	catch (TupleSpaceException tse) {
			throw new DDBSToolkitException("Error executing the middleware request", tse);
		} catch (IOException ioe) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new DDBSToolkitException("Class not found exception", cnfe);
		}
    	catch (Exception e) {
    		throw new DDBSToolkitException("Error executing the middleware request", e);
    	}
    }

    @Override
    public boolean createEntity(IEntity objectToCreate) throws DDBSToolkitException {

    	try
    	{
    		DistributedEntity myDistributedEntity = (DistributedEntity) objectToCreate;

            if(isOpen == true && objectToCreate != null && myDistributedEntity.peerUid != null)
            {

                DDBSCommand command = new DDBSCommand();
                command.setAction(DDBSCommand.CREATE_ENTITY);
                command.setDestination(myDistributedEntity.peerUid);
                command.setObject(objectToCreate);

                TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                Tuple template = new Tuple(Boolean.class);
                Tuple result = resultSpace.waitToTake(template);

                resultSpace.disconnect();

                return (Boolean) result.getField(0).getValue();
            }
            else
            {
                return false;
            }
    	}
    	catch (TupleSpaceException tse) {
			throw new DDBSToolkitException("Error executing the middleware request", tse);
		} catch (IOException ioe) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new DDBSToolkitException("Class not found exception", cnfe);
		}
    	catch (Exception e) {
    		throw new DDBSToolkitException("Error executing the middleware request", e);
    	}
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T extends IEntity> T loadArray(T objectToLoad, String field, String orderBy) throws DDBSToolkitException {

    	try
    	{
    		DistributedEntity myDistributedEntity = (DistributedEntity) objectToLoad;

            //Connection must be established
            if(isOpen == true && objectToLoad != null && myDistributedEntity.peerUid != null && field != null && !field.isEmpty())
            {

                DDBSCommand command = new DDBSCommand();
                command.setAction(DDBSCommand.LOAD_ARRAY_COMMAND);
                command.setDestination(myDistributedEntity.peerUid);
                command.setObject(objectToLoad);
                command.setFieldToLoad(field);
                command.setOrderBy(orderBy);

                TupleID id = commandPeers.write(SqlSpacesConverter.getTuple(command, timeout));

                TupleSpace resultSpace = new TupleSpace(ipAddressServer, port, clusterName+"-results-"+id);
                Tuple template = new Tuple(String.class);
                Tuple result = resultSpace.waitToTake(template);

                resultSpace.disconnect();

                return (T) SqlSpacesConverter.fromString((String) result.getField(0).getValue());
            }
            else
            {
                return null;
            }
    	}
    	catch (TupleSpaceException tse) {
			throw new DDBSToolkitException("Error executing the middleware request", tse);
		} catch (IOException ioe) {
			throw new DDBSToolkitException("Error executing the middleware request - IO Exception", ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new DDBSToolkitException("Class not found exception", cnfe);
		}
    	catch (Exception e) {
    		throw new DDBSToolkitException("Error executing the middleware request", e);
    	}
    }

    @Override
    public List<Peer> getListPeers() throws Exception {

        Tuple template = new Tuple(String.class);

        Tuple[] list = spacePeers.readAll(template);

        ArrayList<Peer> listPeers = new ArrayList<Peer>();
        for(int i = 0; i < list.length; i++)
        {
            String peerString = (String)list[i].getField(0).getValue();
            listPeers.add((Peer) SqlSpacesConverter.fromString(peerString));
        }
        return listPeers;
    }
}
