package com.tngtech.archunit.example.layers.persistence.second.dao.jpa;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.tngtech.archunit.example.layers.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.layers.persistence.second.dao.domain.OtherPersistentObject;
import com.tngtech.archunit.example.layers.security.Secured;
import com.tngtech.archunit.example.layers.service.ProxiedConnection;

public class OtherJpa implements OtherDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OtherPersistentObject findById(long id) {
        return entityManager.find(OtherPersistentObject.class, id);
    }

    @Override
    public void testConnection() throws SQLException {
        Connection conn = entityManager.unwrap(Connection.class);
        if (conn instanceof ProxiedConnection) {
            ((ProxiedConnection) conn).refresh();
        }
        conn.prepareStatement("SELECT 1 FROM DUAL");
    }

    @Override
    @Secured
    public EntityManager getEntityManager() {
        return entityManager;
    }
}
