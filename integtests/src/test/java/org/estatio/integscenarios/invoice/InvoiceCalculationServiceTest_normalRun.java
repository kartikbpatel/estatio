/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.integscenarios.invoice;

import java.math.BigDecimal;
import java.util.SortedSet;
import javax.inject.Inject;
import org.estatio.dom.invoice.InvoiceStatus;
import org.estatio.dom.lease.*;
import org.estatio.dom.lease.invoicing.*;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset.PropertiesAndUnitsFixture;
import org.estatio.fixture.lease.LeasesAndLeaseUnitsAndLeaseItemsAndLeaseTermsAndTagsAndBreakOptionsFixture;
import org.estatio.fixture.party.PersonsAndOrganisationsAndCommunicationChannelsFixture;
import org.estatio.integtests.EstatioIntegrationTest;
import org.estatio.services.settings.EstatioSettingsService;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.apache.isis.applib.fixturescripts.CompositeFixtureScript;
import org.apache.isis.objectstore.jdo.applib.service.support.IsisJdoSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * This looks to have been copied-n-pasted from {@link InvoiceCalculationServiceTest_normalRun_COPY};
 * both have ignored tests; not sure which is in the best state to fix up.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InvoiceCalculationServiceTest_normalRun extends EstatioIntegrationTest {

    @BeforeClass
    public static void setupData() {
        scenarioExecution().install(new CompositeFixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                execute(new EstatioBaseLineFixture(), executionContext);
                execute("parties", new PersonsAndOrganisationsAndCommunicationChannelsFixture(), executionContext);
                execute("properties", new PropertiesAndUnitsFixture(), executionContext);
                execute("leases", new LeasesAndLeaseUnitsAndLeaseItemsAndLeaseTermsAndTagsAndBreakOptionsFixture(), executionContext);
            }
        });
    }

    @Inject
    private Leases leases;
    @Inject
    private LeaseTerms leaseTerms;
    @Inject
    private InvoiceItemsForLease invoiceItemsForLease;
    @Inject
    private EstatioSettingsService estatioSettingsService;
    @Inject
    private IsisJdoSupport isisJdoSupport;

    @Inject
    private InvoiceCalculationService invoiceCalculationService;

    private Lease lease;
    private LeaseItem leaseTopModelRentItem;
    private LeaseItem leaseTopModelServiceChargeItem;

    @Before
    public void setup() {
        lease = leases.findLeaseByReference("OXF-TOPMODEL-001");
        assertThat(lease.getItems().size(), is(3));

        leaseTopModelRentItem = lease.findItem(LeaseItemType.RENT, dt(2010, 7, 15), bi(1));
        leaseTopModelServiceChargeItem = lease.findItem(LeaseItemType.SERVICE_CHARGE, dt(2010, 7, 15), bi(1));

        assertNotNull(leaseTopModelRentItem);
        assertNotNull(leaseTopModelServiceChargeItem);
    }

    @Test
    public void whenLeaseTermApproved() throws Exception {

        // given
        lease.verifyUntil(dt(2014, 1, 1));

        LeaseTerm leaseTopModelRentTerm = leaseTopModelRentItem.findTerm(dt(2010, 7, 15));
        leaseTopModelRentTerm.approve();

        assertThat(leaseTopModelRentTerm.getStatus(), is(LeaseTermStatus.APPROVED));
        assertThat(leaseTopModelRentTerm.getEffectiveValue(), is(bd2(20200)));

        // when, then
        calculateNormalRunAndAssert(leaseTopModelRentTerm, "2010-07-01", "2010-10-01", "2010-07-01/2010-10-01", 4239.13, false);
    }

    // scenario: invoiceItemsForRentCreated
    @Ignore // TODO: to un-ignore
    @Test
    public void t14b_invoiceItemsForRentCreated() throws Exception {

        estatioSettingsService.updateEpochDate(null);

        LeaseTerm leaseTopModelRentTerm0 = (LeaseTerm) leaseTopModelRentItem.getTerms().first();
        // full term
        calculateNormalRunAndAssert(leaseTopModelRentTerm0, "2010-10-01", "2010-10-02", "2010-10-01/2011-01-01", 5000.00, false);
        // invoice after effective date
        calculateNormalRunAndAssert(leaseTopModelRentTerm0, "2010-10-01", "2011-04-02", "2010-10-01/2011-01-01", 5050.00, false);
        // invoice after effective date with mock
        estatioSettingsService.updateEpochDate(dt(2011, 1, 1));

        calculateNormalRunAndAssert(leaseTopModelRentTerm0, "2010-10-01", "2011-04-2", "2010-10-01/2011-01-01", 50.00, true);

        estatioSettingsService.updateEpochDate(null);

        // TODO: without this code there will be 4 items in the set
        isisJdoSupport.refresh(leaseTopModelRentTerm0);
        SortedSet<InvoiceItemForLease> invoiceItems = leaseTopModelRentTerm0.getInvoiceItems();
        assertThat(invoiceItems.size(), is(3));
    }

    // scenario: invoiceItemsForServiceChargeCreated
    @Ignore // TODO: to un-ignore
    @Test
    public void t15_invoiceItemsForServiceChargeCreated() throws Exception {

        estatioSettingsService.updateEpochDate(dt(1980, 1, 1));

        LeaseTermForServiceCharge leaseTopModelServiceChargeTerm0 = (LeaseTermForServiceCharge) leaseTopModelServiceChargeItem.getTerms().first();
        leaseTopModelServiceChargeTerm0.approve();

        // partial period
        calculateNormalRunAndAssert(leaseTopModelServiceChargeTerm0, "2010-07-01", "2010-10-01", "2010-07-01/2010-10-01", 1271.74, false);
        // full period

        calculateNormalRunAndAssert(leaseTopModelServiceChargeTerm0, "2010-01-10", "2010-10-02", "2010-10-01/2011-01-01", 1500.00, false);
        // reconcile with mock date
    }

    @Ignore // TODO: to un-ignore
    @Test
    public void t15_invoiceItemsForServiceChargeCreatedWithEpochDate() throws Exception {
        estatioSettingsService.updateEpochDate(dt(2011, 1, 1));
        LeaseTermForServiceCharge leaseTopModelServiceChargeTerm0 = (LeaseTermForServiceCharge) leaseTopModelServiceChargeItem.getTerms().first();

        calculateNormalRunAndAssert(leaseTopModelServiceChargeTerm0, "2010-10-01", "2011-01-01", "2010-10-01/2011-01-01", 0.00, false);
        leaseTopModelServiceChargeTerm0.setAuditedValue(bd(6600.00));
        leaseTopModelServiceChargeTerm0.verify();

        calculateNormalRunAndAssert(leaseTopModelServiceChargeTerm0, "2010-10-01", "2012-01-01", "2010-10-01/2011-01-01", 150.00, true);
        // reconcile without mock
        estatioSettingsService.updateEpochDate(dt(1980, 1, 1));
    }

    @Ignore // TODO: to un-ignore
    @Test
    public void t16_bulkLeaseCalculate() throws Exception {
        leaseTopModelServiceChargeItem = lease.findItem(LeaseItemType.SERVICE_CHARGE, dt(2010, 7, 15), bi(1));
        LeaseTermForServiceCharge leaseTopModelServiceChargeTerm0 = (LeaseTermForServiceCharge) leaseTopModelServiceChargeItem.getTerms().first();
        // call calculate on leaseTopModel
        lease.calculate(InvoiceRunType.NORMAL_RUN, InvoiceCalculationSelection.RENT_AND_SERVICE_CHARGE, dt(2010, 10, 1), dt(2010, 10, 1), null);
        assertThat(leaseTopModelServiceChargeTerm0.getInvoiceItems().size(), is(2));
    }

    private void calculateNormalRunAndAssert(
            final LeaseTerm leaseTerm,
            final String startDueDate,
            final String nextDueDate,
            final String interval,
            final Double expected,
            final boolean expectedAdjustment) {

        invoiceItemsForLease.removeUnapprovedInvoiceItems(leaseTerm, ival(interval));

        nextTransaction();
        isisJdoSupport.refresh(leaseTerm);

        InvoiceCalculationParameters parameters = new InvoiceCalculationParameters(
                leaseTerm,
                InvoiceRunType.NORMAL_RUN,
                dt(startDueDate),
                dt(startDueDate),
                dt(nextDueDate));
        invoiceCalculationService.calculateAndInvoice(parameters);

        InvoiceItemForLease invoiceItem = invoiceItemsForLease.findUnapprovedInvoiceItem(leaseTerm, ival(interval));
        isisJdoSupport.refresh(leaseTerm);

        BigDecimal netAmount = invoiceItem == null ? bd2(0) : invoiceItem.getNetAmount();
        final String reason = "size " + invoiceItemsForLease.findByLeaseTermAndInvoiceStatus(leaseTerm, InvoiceStatus.NEW).size();
        assertThat(reason, netAmount, is(bd2hup(expected)));

        Boolean adjustment = invoiceItem == null ? false : invoiceItem.isAdjustment();
        assertThat(adjustment, is(expectedAdjustment));
    }



}