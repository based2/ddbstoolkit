package org.ddbstoolkit.toolkit.modules.datastore.jena.model;

import org.ddbstoolkit.toolkit.core.DistributedEntity;
import org.ddbstoolkit.toolkit.core.Id;
import org.ddbstoolkit.toolkit.core.reflexion.PropertyName;
import org.ddbstoolkit.toolkit.modules.datastore.jena.annotation.DefaultNamespace;
import org.ddbstoolkit.toolkit.modules.datastore.jena.annotation.Optional;
import org.ddbstoolkit.toolkit.modules.datastore.jena.annotation.Service;
import org.ddbstoolkit.toolkit.modules.datastore.jena.annotation.URI;

/**
 * Class representing a genre of book
 * @author Cyril GRANDJEAN
 * @version 1.0 Creation of the class
 */
@SuppressWarnings("serial")
@Service(url="http://www.factforge.net/sparql")
@DefaultNamespace(name="fb",url="http://rdf.freebase.com/ns/")
public class Genre extends DistributedEntity {

    @Id
    @Optional
    public int genre_id;

    @URI
    public String genre_uri;

    @PropertyName(name = "type.object.name")
    public String name;
}

