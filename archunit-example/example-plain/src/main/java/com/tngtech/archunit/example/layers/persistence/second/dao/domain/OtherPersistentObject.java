package com.tngtech.archunit.example.layers.persistence.second.dao.domain;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class OtherPersistentObject {
    @Id
    private long id;

    OtherPersistentObject() {
    }

    public OtherPersistentObject(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final OtherPersistentObject other = (OtherPersistentObject) obj;
        return Objects.equals(this.id, other.id);
    }
}
