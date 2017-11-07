package org.ctlv.proxmox.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.json.JSONException;

public class Monitor implements Runnable {

	private Analyzer analyzer;
	private ProxmoxAPI api;
	private static Object LOCK;

	public Monitor(ProxmoxAPI api, Analyzer analyzer, Object LOCK) {
		this.api = api;
		this.analyzer = analyzer;
		Monitor.LOCK = LOCK;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e2) {
		}
		
		while (true) {
			synchronized (Monitor.LOCK) {
				// Récupérer les données sur les serveurs
				Map<String, List<LXC>> myCTsPerServer = new HashMap<String, List<LXC>>();
				myCTsPerServer.put(Constants.SERVER1, new ArrayList<LXC>());
				myCTsPerServer.put(Constants.SERVER2, new ArrayList<LXC>());
				try {
					for (LXC ct : api.getCTs(Constants.SERVER1))
						myCTsPerServer.get(Constants.SERVER1).add(ct);

					for (LXC ct : api.getCTs(Constants.SERVER2))
						myCTsPerServer.get(Constants.SERVER2).add(ct);

				} catch (LoginException | JSONException | IOException e1) {
					e1.printStackTrace();
				}

				// Lancer l'anlyse
				analyzer.analyze(myCTsPerServer);
			}
			// attendre une certaine période
			try {
				Thread.sleep(Constants.MONITOR_PERIOD * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
