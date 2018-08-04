package com.tngtech.archunit.example.service;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.tngtech.archunit.example.MyService;
import com.tngtech.archunit.example.persistence.first.dao.SomeDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.persistence.second.dao.domain.OtherPersistentObject;

@MyService
public class ServiceViolatingDaoRules {
    @EJB
    private SomeDao someDao;
    @EJB
    private OtherDao otherDao;
    @PersistenceContext
    private MyEntityManager myEntityManager;

    public void doSthService() {
        someDao.findById(0);
    }

    public void illegallyUseEntityManager() {
        otherDao.getEntityManager().persist(new OtherPersistentObject(1L)); // Violates rule not to use EntityManager outside of DAOs
        myEntityManager.persist(new OtherPersistentObject(2L)); // Violates rule not to use EntityManager outside of DAOs
    }

    public abstract static class MyEntityManager implements EntityManager {
    }
}
