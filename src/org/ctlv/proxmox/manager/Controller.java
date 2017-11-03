package org.ctlv.proxmox.manager;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.json.JSONException;

public class Controller {

	ProxmoxAPI api;

	public Controller(ProxmoxAPI api) {
		this.api = api;
	}

	// migrer un conteneur du serveur "srcServer" vers le serveur "dstServer"
	public void migrateFromTo(String srcServer, String dstServer) throws LoginException, JSONException, IOException {
		List<String> CTList = api.getCTList(srcServer);
		if (!CTList.isEmpty())
			api.migrateCT(srcServer, CTList.get(0), dstServer);
		else
			throw new NoSuchElementException(
					"Impossible de migrer une CT depuis le serveur " + srcServer + " car il n'en a aucune.");
	}

	// arrêter le plus vieux conteneur sur le serveur "server"
	public void offLoad(String server) throws LoginException, JSONException, IOException {
		List<LXC> CTs = api.getCTs(server);
		CTs.sort(new Comparator<LXC>() {
			// On trie de sorte à avoir la CT la plus ancienne en première, donc un tri
			// croissant avec "vieille CT < nouvelle CT"
			@Override
			public int compare(LXC o1, LXC o2) {
				return (int) (o2.getUptime() - o1.getUptime());
			}
		});
		if (!CTs.isEmpty())
			api.stopCT(server, CTs.get(0).getVmid());
		else
			throw new NoSuchElementException(
					"Impossible d'arrêter une CT sur le serveur " + server + " car il n'y en a aucune.");
	}

}
