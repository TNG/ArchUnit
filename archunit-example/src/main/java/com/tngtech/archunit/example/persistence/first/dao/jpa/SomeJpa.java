package com.tngtech.archunit.example.persistence.first.dao.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.tngtech.archunit.example.persistence.first.dao.SomeDao;
import com.tngtech.archunit.example.persistence.first.dao.domain.PersistentObject;

public class SomeJpa implements SomeDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PersistentObject findById(long id) {
        return entityManager.find(PersistentObject.class, id);
    }
}
