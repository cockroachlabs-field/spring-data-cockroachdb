package org.springframework.data.cockroachdb.it.bank.model;

public class ForeignSystemJsonType extends AbstractJsonDataType<ForeignSystem> {
    @Override
    public Class<ForeignSystem> returnedClass() {
        return ForeignSystem.class;
    }

    @Override
    public boolean isCollectionType() {
        return false;
    }
}
