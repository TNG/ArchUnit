package com.tngtech.archunit.example.persistence.second.dao.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.persistence.second.dao.domain.OtherPersistentObject;
import com.tngtech.archunit.example.security.Secured;

import java.sql.Connection;
import java.sql.SQLException;

public class OtherJpa implements OtherDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OtherPersistentObject findById(long id) {
        return entityManager.find(OtherPersistentObject.class, id);
    }

    @Override
    public void testConnection() throws SQLException {
        Connection conn = (Connection) entityManager.unwrap(java.sql.Connection.class);
        conn.prepareStatement("SELECT 1 FROM DUAL");
    }

    @Override
    @Secured
    public EntityManager getEntityManager() {
        return entityManager;
    }
}
