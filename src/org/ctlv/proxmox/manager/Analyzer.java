package org.ctlv.proxmox.manager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.json.JSONException;

public class Analyzer {
	ProxmoxAPI api;
	Controller controller;

	public Analyzer(ProxmoxAPI api, Controller controller) {
		this.api = api;
		this.controller = controller;
	}

	public void analyze(Map<String, List<LXC>> myCTsPerServer) {

		try {
			System.out.println("Analyse des CTs...");

			// Mémoire autorisée sur chaque serveur
			float totalMemServer1 = (long) (api.getNode(Constants.SERVER1).getMemory_total());
			float totalMemServer2 = (long) (api.getNode(Constants.SERVER2).getMemory_total());

			// Analyze et Actions
			// On répartit la charge entre les serveurs
			while (memOnServer(myCTsPerServer.get(Constants.SERVER1)) > Constants.MIGRATION_THRESHOLD * totalMemServer1
					&& memOnServer(myCTsPerServer.get(Constants.SERVER2)) < Constants.MIGRATION_THRESHOLD
							* totalMemServer2) {
				System.out.println(Constants.SERVER1 + " est surchargé, migration d'une CT vers " + Constants.SERVER2);
				controller.migrateFromTo(Constants.SERVER1, Constants.SERVER2);
			}

			while (memOnServer(myCTsPerServer.get(Constants.SERVER2)) > Constants.MIGRATION_THRESHOLD * totalMemServer2
					&& memOnServer(myCTsPerServer.get(Constants.SERVER1)) < Constants.MIGRATION_THRESHOLD
							* totalMemServer1) {
				System.out.println(Constants.SERVER2 + " est surchargé, migration d'une CT vers " + Constants.SERVER1);
				controller.migrateFromTo(Constants.SERVER2, Constants.SERVER1);
			}

			// On arrête les CTs les plus anciens si un serveur est au dessus du seuil
			while (memOnServer(myCTsPerServer.get(Constants.SERVER1)) > Constants.DROPPING_THRESHOLD
					* totalMemServer1) {
				System.out.println("Arrêt d'une CT sur " + Constants.SERVER1);
				controller.offLoad(Constants.SERVER1);
			}
			while (memOnServer(myCTsPerServer.get(Constants.SERVER2)) > Constants.DROPPING_THRESHOLD
					* totalMemServer2) {
				System.out.println("Arrêt d'une CT sur " + Constants.SERVER2);
				controller.offLoad(Constants.SERVER2);
			}

			System.out.println("Fin de l'analyse");
		} catch (LoginException | JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	// Calculer la quantité de RAM utilisée par mes CTs sur un serveur
	private long memOnServer(List<LXC> myCTsOnServer) throws LoginException, JSONException, IOException {
		long usedMem = 0;
		for (LXC ct : myCTsOnServer)
			if (ct.getName().contains(Constants.CT_BASE_NAME))
				usedMem += ct.getMem();

		return usedMem;
	}

}
