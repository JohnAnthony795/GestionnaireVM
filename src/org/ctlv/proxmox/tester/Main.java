package org.ctlv.proxmox.tester;

import java.io.IOException;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.ctlv.proxmox.api.data.Node;
import org.json.JSONException;

public class Main {

	public static void main(String[] args) throws LoginException, JSONException, IOException {

		ProxmoxAPI api = new ProxmoxAPI();

		for (String s : api.getNodes()) {
			Node n = api.getNode(s);
			System.out.println("Serveur " + s + " :\nCPU : " + n.getCpu() * 100 + "%\nRAM : "
					+ n.getMemory_used() / 1000000 + "MB/" + n.getMemory_total() / 1000000 + "MB\nDisk : "
					+ n.getRootfs_free() / 1000000 + "MB/" + n.getRootfs_total() / 1000000 + "MB");
		}

		print(api);
		// api.createCT(Constants.SERVER1, Long.toString(Constants.CT_BASE_ID),
		// Constants.CT_BASE_NAME + Constants.CT_BASE_ID%100, Constants.RAM_SIZE[1]);
		api.startCT(Constants.SERVER1, Long.toString(Constants.CT_BASE_ID));
		LXC ct = api.getCT(Constants.SERVER1, Long.toString(Constants.CT_BASE_ID));
		System.out.println("cpu : " + ct.getCpu() + "\nstatus : " + ct.getStatus());
		print(api);

	}

	public static void print(ProxmoxAPI api) throws LoginException, JSONException, IOException {
		List<LXC> cts = api.getCTs(Constants.SERVER1);
		System.out.println("Boucle");
		for (LXC lxc : cts) {
			System.out.println(lxc.getName());
		}
		System.out.println("Fin boucle");
	}

}
