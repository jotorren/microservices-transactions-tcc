package net.jotorren.microservices.content.dao;

import net.jotorren.microservices.content.domain.SourceCodeItem;

import org.springframework.data.repository.CrudRepository;

public interface ContentDao extends CrudRepository<SourceCodeItem, String> {

}
