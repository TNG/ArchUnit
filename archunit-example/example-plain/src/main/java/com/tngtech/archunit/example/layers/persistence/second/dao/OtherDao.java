package com.tngtech.archunit.example.layers.persistence.second.dao;

import java.sql.SQLException;

import javax.persistence.EntityManager;

import com.tngtech.archunit.example.layers.persistence.second.dao.domain.OtherPersistentObject;

public interface OtherDao {
    OtherPersistentObject findById(long id);

    void testConnection() throws SQLException;

    EntityManager getEntityManager();
}
