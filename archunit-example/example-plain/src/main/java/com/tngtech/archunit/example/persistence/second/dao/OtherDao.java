package com.tngtech.archunit.example.persistence.second.dao;

import javax.persistence.EntityManager;

import com.tngtech.archunit.example.persistence.second.dao.domain.OtherPersistentObject;

import java.sql.SQLException;

public interface OtherDao {
    OtherPersistentObject findById(long id);

    void testConnection() throws SQLException;

    EntityManager getEntityManager();
}
