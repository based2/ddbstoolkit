package org.ddbstoolkit.toolkit.jdbc.model;

import org.ddbstoolkit.toolkit.core.annotations.EntityName;
import org.ddbstoolkit.toolkit.core.annotations.Id;
import org.ddbstoolkit.toolkit.model.interfaces.ActorBase;

/**
 * Entity representing an actor
 * @author Cyril GRANDJEAN
 * @version 1.0 Creation of the class
 */
@SuppressWarnings("serial")
public class Actor extends ActorBase {

    @Id
    @EntityName(name="actor_id")
    private Integer actorId;

    @EntityName(name="actor_name")
    private String actorName;

    @EntityName(name="film_id")
    private Integer filmId;
    
    public Actor() {
		super();
	}

	public Actor(Integer actorId, String actorName, Integer filmId) {
		super();
		this.actorId = actorId;
		this.actorName = actorName;
		this.filmId = filmId;
	}

	public Integer getActorId() {
		return actorId;
	}

	public void setActorId(Integer actorId) {
		this.actorId = actorId;
	}

	public String getActorName() {
		return actorName;
	}

	public void setActorName(String actorName) {
		this.actorName = actorName;
	}

	public Integer getFilmId() {
		return filmId;
	}

	public void setFilmId(Integer filmId) {
		this.filmId = filmId;
	}
}
