package com.tngtech.archunit.example.persistence.first.dao;

import com.tngtech.archunit.example.persistence.first.dao.domain.PersistentObject;

public interface SomeDao {
    PersistentObject findById(long id);
}
