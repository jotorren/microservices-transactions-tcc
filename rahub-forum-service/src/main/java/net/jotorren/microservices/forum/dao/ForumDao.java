package net.jotorren.microservices.forum.dao;

import net.jotorren.microservices.forum.domain.Forum;

import org.springframework.data.repository.CrudRepository;

public interface ForumDao extends CrudRepository<Forum, String> {

}
