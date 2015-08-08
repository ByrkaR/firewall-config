package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.HostInterface;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.util.Printer;
import com.payneteasy.firewall.util.StringAppender;
import com.payneteasy.firewall.util.UniqueStringAppender;

import java.io.File;
import java.io.IOException;

import static com.payneteasy.firewall.util.Printer.out;

public class MainMikrotik {

    final IConfigDao dao;
    public MainMikrotik(IConfigDao configDao) {
        dao = configDao;
    }

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        String host = args[1];
        String vlan = args[2];

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        new MainMikrotik(configDao).showVlanConfig(host, vlan);
    }


    private void showVlanConfig(String aHostname, String aVlan) {
        THost host = dao.getHostByName(aHostname);
        out("# %s %s", aHostname, aVlan);
        out();
        out("/interface ethernet switch egress-vlan-tag");
        out("add tagged-ports=%s vlan-id=%s", findTrunk(host), aVlan);
        out();
        out("/interface ethernet switch ingress-vlan-translation");
        out("add new-customer-vid=%s ports=%s sa-learning=yes", aVlan, findVlanPorts(host, aVlan));
        out();
        out("/interface ethernet switch vlan");
        out("add ports=%s,%s vlan-id=%s", findTrunk(host), findVlanPorts(host, aVlan), aVlan);
    }

    private String findVlanPorts(THost aHost, String aVlan) {
        StringAppender sb = new UniqueStringAppender(",");
        for (TInterface iface : aHost.interfaces) {
            if(aVlan.equals(iface.vlan)) {
                sb.append(iface.name);
            } else if(iface.vlan == null) {
                // finds linked switch and get VLAN from it
                HostInterface linkedInterface = dao.findLinkedInterface(aHost, iface);
                if(linkedInterface!= null && aVlan.equals(linkedInterface.iface.vlan)) {
                    sb.append(iface.name);
                }
            }
        }
        return sb.toStringFailIfEmpty("Could not find interfaces with VLAN "+aVlan+" in host "+aHost.name);
    }

    private String findTrunk(THost aHost) {
        for (TInterface iface : aHost.interfaces) {
            if("trunk".equals(iface.vlan)) {
                return iface.name;
            }
        }
        throw new IllegalStateException("trunk port not found in "+aHost.name);
    }

}