package org.estatio.dom.apptenancy;

import org.apache.isis.applib.annotation.Programmatic;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;
import org.isisaddons.module.security.dom.tenancy.HasAtPath;

public interface WithApplicationTenancy extends HasAtPath {

    ApplicationTenancy getApplicationTenancy();

    @Programmatic
    @Override
    String getAtPath();
}
