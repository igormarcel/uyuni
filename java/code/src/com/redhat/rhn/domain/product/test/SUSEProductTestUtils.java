/**
 * Copyright (c) 2014 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.redhat.rhn.domain.product.test;

import com.redhat.rhn.common.db.datasource.DataResult;
import com.redhat.rhn.common.db.datasource.ModeFactory;
import com.redhat.rhn.common.db.datasource.SelectMode;
import com.redhat.rhn.common.db.datasource.WriteMode;
import com.redhat.rhn.common.hibernate.HibernateFactory;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelArch;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.channel.ChannelFamily;
import com.redhat.rhn.domain.channel.test.ChannelFactoryTest;
import com.redhat.rhn.domain.channel.test.ChannelFamilyFactoryTest;
import com.redhat.rhn.domain.product.SUSEProduct;
import com.redhat.rhn.domain.rhnpackage.PackageFactory;
import com.redhat.rhn.domain.server.Server;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.testing.ChannelTestUtils;
import com.redhat.rhn.testing.TestUtils;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Utility methods for creating SUSE related test data.
 */
public class SUSEProductTestUtils extends HibernateFactory {

    private static Logger log = Logger.getLogger(SUSEProductTestUtils.class);

    /**
     * Not to be instantiated.
     */
    private SUSEProductTestUtils() {
    }

    /**
     * Create a SUSE product (which is different from a {@link com.redhat.rhn.domain.channel.ChannelProduct}).
     * @param family the channel family
     * @return the newly created SUSE product
     * @throws Exception if anything goes wrong
     */
    public static SUSEProduct createTestSUSEProduct(ChannelFamily family) throws Exception {
        SUSEProduct product = new SUSEProduct();
        String name = TestUtils.randomString().toLowerCase();
        product.setName(name);
        product.setVersion("1");
        product.setFriendlyName("SUSE Test product " + name);
        product.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        product.setRelease("test");
        product.setProductId(new Random().nextInt(999999));

        TestUtils.saveAndReload(product);

        return product;
    }

    /**
     * Create a vendor channel (org is null) for testing.
     * @return vendor channel for testing
     * @throws Exception
     */
    public static Channel createTestVendorChannel() throws Exception {
        Channel c = ChannelFactoryTest.createTestChannel(null,
                ChannelFamilyFactoryTest.createTestChannelFamily());
        ChannelFactory.save(c);
        return c;
    }

    /**
     * For a given file, delete it in case it is a temp file.
     * @param file test file to delete
     */
    public static void deleteIfTempFile(File file) {
        if (file.exists() && file.getAbsolutePath().startsWith(
                System.getProperty("java.io.tmpdir") + File.separatorChar)) {
            file.delete();
        }
    }

    /**
     * Create a SUSE product channel (that is, links a channel to a SUSE
     * product, eg. a row in suseproductchannel).
     * @param channel the channel
     * @param product the SUSE product
     */
    public static void createTestSUSEProductChannel(Channel channel, SUSEProduct product) {
        WriteMode m = ModeFactory.getWriteMode("test_queries",
                "insert_into_suseproductchannel");

        Channel parentChannel = channel.getParentChannel();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("product_id", product.getId());
        parameters.put("channel_id", channel.getId());
        parameters.put("channel_label", channel.getLabel());
        parameters.put("parent_channel_label",
                parentChannel != null ? parentChannel.getLabel() : null);

        m.executeUpdate(parameters);
        HibernateFactory.getSession().flush();
    }

    /**
     * Create a SUSE product channel (that is, links a channel to a SUSE
     * product, eg. a row in suseproductchannel) for a not synced channel
     * @param channelLabel the channel label
     * @param parentChannelLabel the parent channel label
     * @param product the SUSE product
     */
    public static void createTestSUSEProductChannelNotSynced(String channelLabel, String parentChannelLabel, SUSEProduct product) {
        WriteMode m = ModeFactory.getWriteMode("test_queries",
                "insert_into_suseproductchannel");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("product_id", product.getId());
        parameters.put("channel_id", null);
        parameters.put("channel_label", channelLabel);
        parameters.put("parent_channel_label", parentChannelLabel);

        m.executeUpdate(parameters);
        HibernateFactory.getSession().flush();
    }

    /**
     * Marks one SUSE product as a possible upgrade of another.
     * @param from the first SUSE product
     * @param to the second SUSE product
     */
    public static void createTestSUSEUpgradePath(SUSEProduct from, SUSEProduct to) {
        WriteMode m = ModeFactory.getWriteMode("test_queries",
                "insert_into_suseupgradepath");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("from_pdid", from.getId());
        parameters.put("to_pdid", to.getId());

        m.executeUpdate(parameters);
        HibernateFactory.getSession().flush();
    }

    /**
     * Link a {@link com.redhat.rhn.domain.product.SUSEProduct}
     * with a {@link com.redhat.rhn.domain.server.Server}.
     * @param product the product
     * @param server the server
     */
    @SuppressWarnings("unchecked")
    public static void installSUSEProductOnServer(SUSEProduct product, Server server) {
        // Get the next id from the sequence
        SelectMode selectMode = ModeFactory.getMode("test_queries",
                "select_next_suseinstalledproduct_id");
        DataResult<Map<String, Long>> dataResults = selectMode.execute();
        Long installedProductId = dataResults.get(0).get("id");

        // Insert into suseInstalledProduct
        WriteMode writeMode1 = ModeFactory.getWriteMode("test_queries",
                "insert_into_suseinstalledproduct");
        Map<String, Object> params1 = new HashMap<String, Object>();
        params1.put("id", installedProductId);
        params1.put("name", product.getName());
        params1.put("version", product.getVersion());
        params1.put("arch_type_id", product.getArch().getId());
        params1.put("release", product.getRelease());
        params1.put("is_baseproduct", "Y");
        writeMode1.executeUpdate(params1);

        // Insert into suseServerInstalledProduct
        WriteMode writeMode2 = ModeFactory.getWriteMode("test_queries",
                "insert_into_suseserverinstalledproduct");
        Map<String, Object> params2 = new HashMap<String, Object>();
        params2.put("rhn_server_id", server.getId());
        params2.put("suse_installed_product_id", installedProductId);
        writeMode2.executeUpdate(params2);
        HibernateFactory.getSession().flush();
    }

    /**
     * Create two standard SUSE Vendor products.
     *
     * SLES12 SP1 x86_64
     * SLE-HA12 SP1 x86_64
     */
    public static void createVendorSUSEProducts() {
        SUSEProduct product = new SUSEProduct();
        product.setName("sles");
        product.setVersion("12.1");
        product.setFriendlyName("SUSE Linux Enterprise Server 12 SP1");
        product.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        product.setProductId(1322);
        TestUtils.saveAndFlush(product);

        product = new SUSEProduct();
        product.setName("sle-ha");
        product.setVersion("12.1");
        product.setFriendlyName("SUSE Linux Enterprise High Availability Extension 12 SP1");
        product.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        product.setProductId(1324);
        TestUtils.saveAndFlush(product);
    }

    public static Channel createBaseChannelForBaseProduct(SUSEProduct product, User admin) throws Exception {
        ChannelArch channelArch = ChannelFactory.findArchByLabel("channel-x86_64");
        Channel channel = ChannelTestUtils.createBaseChannel(admin);
        channel.setChannelArch(channelArch);
        ChannelFactory.save(channel);
        SUSEProductTestUtils.createTestSUSEProductChannel(channel, product);
        return channel;
    }

    public static void createChildChannelsForProduct(SUSEProduct product, Channel baseChannel, User admin) throws Exception {
        ChannelArch channelArch = ChannelFactory.findArchByLabel("channel-x86_64");
        Channel channel = ChannelFactoryTest.createTestChannel(admin);
        channel.setChannelArch(channelArch);
        channel.setParentChannel(baseChannel);
        ChannelFactory.save(channel);
        SUSEProductTestUtils.createTestSUSEProductChannel(channel, product);
    }

    /**
     * Create some SUSE Vendor products with channels
     *
     * SLES12 SP1 x86_64
     * SLE-HA12 SP1 x86_64
     * @param admin
     * @throws Exception
     */
    public static void createVendorSUSEProductEnvironment(User admin) throws Exception {
        SUSEProduct productSLES12 = new SUSEProduct();
        productSLES12.setName("sles");
        productSLES12.setVersion("12");
        productSLES12.setFriendlyName("SUSE Linux Enterprise Server 12");
        productSLES12.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productSLES12.setProductId(1117);

        SUSEProduct productHA12 = new SUSEProduct();
        productHA12.setName("sle-ha");
        productHA12.setVersion("12");
        productHA12.setFriendlyName("SUSE Linux Enterprise High Availability Extension 12");
        productHA12.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productHA12.setProductId(1245);

        SUSEProduct productHAGEO12 = new SUSEProduct();
        productHAGEO12.setName("sle-ha-geo");
        productHAGEO12.setVersion("12");
        productHAGEO12.setFriendlyName("SUSE Linux Enterprise High Availability GEO Extension 12");
        productHAGEO12.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productHAGEO12.setProductId(1157);

        SUSEProduct productSLES121 = new SUSEProduct();
        productSLES121.setName("sles");
        productSLES121.setVersion("12.1");
        productSLES121.setFriendlyName("SUSE Linux Enterprise Server 12 SP1");
        productSLES121.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productSLES121.setProductId(1322);

        SUSEProduct productHA121 = new SUSEProduct();
        productHA121.setName("sle-ha");
        productHA121.setVersion("12.1");
        productHA121.setFriendlyName("SUSE Linux Enterprise High Availability Extension 12 SP1");
        productHA121.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productHA121.setProductId(1324);

        SUSEProduct productHAGEO121 = new SUSEProduct();
        productHAGEO121.setName("sle-ha-geo");
        productHAGEO121.setVersion("12.1");
        productHAGEO121.setFriendlyName("SUSE Linux Enterprise High Availability GEO Extension 12 SP1");
        productHAGEO121.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productHAGEO121.setProductId(1337);

        SUSEProduct productSLES122 = new SUSEProduct();
        productSLES122.setName("sles");
        productSLES122.setVersion("12.2");
        productSLES122.setFriendlyName("SUSE Linux Enterprise Server 12 SP2");
        productSLES122.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productSLES122.setProductId(1357);

        SUSEProduct productHA122 = new SUSEProduct();
        productHA122.setName("sle-ha");
        productHA122.setVersion("12.2");
        productHA122.setFriendlyName("SUSE Linux Enterprise High Availability Extension 12 SP2");
        productHA122.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productHA122.setProductId(1361);

        SUSEProduct productHAGEO122 = new SUSEProduct();
        productHAGEO122.setName("sle-ha-geo");
        productHAGEO122.setVersion("12.2");
        productHAGEO122.setFriendlyName("SUSE Linux Enterprise High Availability GEO Extension 12 SP2");
        productHAGEO122.setArch(PackageFactory.lookupPackageArchByLabel("x86_64"));
        productHAGEO122.setProductId(1363);

        TestUtils.saveAndFlush(productHAGEO12);
        TestUtils.saveAndFlush(productHAGEO121);
        TestUtils.saveAndFlush(productHAGEO122);
        TestUtils.saveAndFlush(productHA122);
        TestUtils.saveAndFlush(productSLES122);
        TestUtils.saveAndFlush(productHA121);
        TestUtils.saveAndFlush(productSLES121);
        TestUtils.saveAndFlush(productHA12);
        TestUtils.saveAndFlush(productSLES12);

        Set<SUSEProduct> upSLES = new HashSet<SUSEProduct>();
        upSLES.add(productSLES121);
        upSLES.add(productSLES122);
        productSLES12.setUpgrades(upSLES);
        Set<SUSEProduct> upHA = new HashSet<SUSEProduct>();
        upHA.add(productHA121);
        upHA.add(productHA122);
        productHA12.setUpgrades(upHA);
        Set<SUSEProduct> upHAGEO = new HashSet<SUSEProduct>();
        upHA.add(productHAGEO121);
        upHA.add(productHAGEO122);
        productHAGEO12.setUpgrades(upHAGEO);

        productSLES121.setDowngrades(Collections.singleton(productSLES12));
        productHA121.setDowngrades(Collections.singleton(productHA12));
        productHAGEO121.setDowngrades(Collections.singleton(productHAGEO12));

        Set<SUSEProduct> downSLES = new HashSet<SUSEProduct>();
        downSLES.add(productSLES121);
        downSLES.add(productSLES12);
        productSLES122.setDowngrades(downSLES);
        Set<SUSEProduct> downHA = new HashSet<SUSEProduct>();
        downHA.add(productHA121);
        downHA.add(productHA12);
        productHA122.setDowngrades(downHA);
        Set<SUSEProduct> downHAGEO = new HashSet<SUSEProduct>();
        downHAGEO.add(productHAGEO121);
        downHAGEO.add(productHAGEO12);
        productHAGEO122.setDowngrades(downHAGEO);

        productSLES12.setExtensionFor(Collections.singleton(productHA12));
        productSLES121.setExtensionFor(Collections.singleton(productHA121));
        productSLES122.setExtensionFor(Collections.singleton(productHA122));

        productHA12.setExtensionFor(Collections.singleton(productHAGEO12));
        productHA121.setExtensionFor(Collections.singleton(productHAGEO121));
        productHA122.setExtensionFor(Collections.singleton(productHAGEO122));

        productHA12.setExtensionOf(Collections.singleton(productSLES12));
        productHA121.setExtensionOf(Collections.singleton(productSLES121));
        productHA122.setExtensionOf(Collections.singleton(productSLES122));

        productHAGEO12.setExtensionOf(Collections.singleton(productHA12));
        productHAGEO121.setExtensionOf(Collections.singleton(productHA121));
        productHAGEO122.setExtensionOf(Collections.singleton(productHA122));

        TestUtils.saveAndReload(productHAGEO122);
        TestUtils.saveAndReload(productHAGEO121);
        TestUtils.saveAndReload(productHAGEO12);
        TestUtils.saveAndReload(productHA122);
        TestUtils.saveAndReload(productSLES122);
        TestUtils.saveAndReload(productHA121);
        TestUtils.saveAndReload(productSLES121);
        TestUtils.saveAndReload(productHA12);
        TestUtils.saveAndReload(productSLES12);

        Channel bcSLES12 = createBaseChannelForBaseProduct(productSLES12, admin);
        createChildChannelsForProduct(productHA12, bcSLES12, admin);
        createChildChannelsForProduct(productHAGEO12, bcSLES12, admin);
        Channel bcSLES121 = createBaseChannelForBaseProduct(productSLES121, admin);
        createChildChannelsForProduct(productHA121, bcSLES121, admin);
        createChildChannelsForProduct(productHAGEO121, bcSLES121, admin);
        Channel bcSLES122 = createBaseChannelForBaseProduct(productSLES122, admin);
        createChildChannelsForProduct(productHA122, bcSLES122, admin);
        createTestSUSEProductChannelNotSynced("channellabel" + TestUtils.randomString().toLowerCase()
                , bcSLES122.getLabel(), productHAGEO122);
    }

    /**
     * Create standard SUSE Vendor Entitlement products.
     */
    public static void createVendorEntitlementProducts() {
        SUSEProduct product = new SUSEProduct();
        product.setName("suse-manager-mgmt-unlimited-virtual-z");
        product.setVersion("1.2");
        product.setFriendlyName("SUSE Manager Mgmt Unlimited Virtual Z 1.2");
        product.setProductId(1200);
        TestUtils.saveAndFlush(product);

        product = new SUSEProduct();
        product.setName("suse-manager-prov-unlimited-virtual-z");
        product.setVersion("1.2");
        product.setFriendlyName("SUSE Manager Prov Unlimited Virtual Z 1.2");
        product.setProductId(1205);
        TestUtils.saveAndFlush(product);

        product = new SUSEProduct();
        product.setName("suse-manager-mgmt-unlimited-virtual");
        product.setVersion("1.2");
        product.setFriendlyName("SUSE Manager Mgmt Unlimited Virtual 1.2");
        product.setProductId(1078);
        TestUtils.saveAndFlush(product);

        product = new SUSEProduct();
        product.setName("suse-manager-prov-unlimited-virtual");
        product.setVersion("1.2");
        product.setFriendlyName("SUSE Manager Prov Unlimited Virtual 1.2");
        product.setProductId(1204);
        TestUtils.saveAndFlush(product);

        product = new SUSEProduct();
        product.setName("suse-manager-mgmt-single");
        product.setVersion("1.2");
        product.setFriendlyName("SUSE Manager Mgmt Single 1.2");
        product.setProductId(1076);
        TestUtils.saveAndFlush(product);

        product = new SUSEProduct();
        product.setName("suse-manager-prov-single");
        product.setVersion("1.2");
        product.setFriendlyName("SUSE Manager Prov Single 1.2");
        product.setProductId(1097);
        TestUtils.saveAndFlush(product);
    }

    /**
     * Resets all product data.
     */
    public static void clearAllProducts() {
        Session session = getSession();
        session.getNamedQuery("SUSEProductChannel.clear").executeUpdate();
        session.getNamedQuery("SUSEProduct.clear").executeUpdate();
        session.getNamedQuery("SUSEUpgradePath.clear").executeUpdate();
        session.getNamedQuery("SUSEProductExtension.clear").executeUpdate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Logger getLogger() {
        return log;
    }
}
