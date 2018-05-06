package com.tngtech.archunit.example.persistence.second.dao.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.persistence.second.dao.domain.OtherPersistentObject;

public class OtherJpa implements OtherDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OtherPersistentObject findById(long id) {
        return entityManager.find(OtherPersistentObject.class, id);
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }
}
