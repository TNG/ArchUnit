package com.tngtech.archunit.example.persistence.second.dao;

import javax.persistence.EntityManager;

import com.tngtech.archunit.example.persistence.second.dao.domain.OtherPersistentObject;

public interface OtherDao {
    OtherPersistentObject findById(long id);

    EntityManager getEntityManager();
}
