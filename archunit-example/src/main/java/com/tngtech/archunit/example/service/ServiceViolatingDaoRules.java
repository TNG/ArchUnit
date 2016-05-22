package com.tngtech.archunit.example.service;

import javax.ejb.EJB;

import com.tngtech.archunit.example.persistence.first.dao.SomeDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.persistence.second.dao.domain.OtherPersistentObject;

public class ServiceViolatingDaoRules {
    @EJB
    private SomeDao someDao;
    @EJB
    private OtherDao otherDao;

    public void doSthService() {
        someDao.findById(0);
    }

    public void illegallyUseEntityManager() {
        otherDao.getEntityManager().persist(new OtherPersistentObject(1L)); // Violates rule not to use EntityManager outside of DAOs
    }
}
