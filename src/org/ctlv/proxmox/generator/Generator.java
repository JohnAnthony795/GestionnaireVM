package org.ctlv.proxmox.generator;

import java.io.IOException;
import java.util.Date;
import java.util.Random;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.json.JSONException;

public class Generator {

	static Random rndTime = new Random(new Date().getTime());

	public static int getNextEventPeriodic(int period) {
		return period;
	}

	public static int getNextEventUniform(int max) {
		return rndTime.nextInt(max);
	}

	public static int getNextEventExponential(int inv_lambda) {
		float next = (float) (-Math.log(rndTime.nextFloat()) * inv_lambda);
		return (int) next;
	}

	public static void run(ProxmoxAPI api, Object LOCK)
			throws InterruptedException, LoginException, JSONException, IOException {
		int timeToWait = 0;

		long baseID = Constants.CT_BASE_ID;
		long ctNum = 0;

		String baseCt = Constants.CT_BASE_NAME;

		Random rndServer = new Random(new Date().getTime());

		long memAllowedOnServer1 = (long) (api.getNode(Constants.SERVER1).getMemory_total() * Constants.MAX_THRESHOLD);
		long memAllowedOnServer2 = (long) (api.getNode(Constants.SERVER2).getMemory_total() * Constants.MAX_THRESHOLD);

		while (true) {

			// 1. Calculer la quantité de RAM utilisée par mes CTs sur chaque serveur
			long memOnServer1 = 0;
			long memOnServer2 = 0;

			synchronized (LOCK) {
				for (LXC ct : api.getCTs(Constants.SERVER1))
					if (isOurCT(ct))
						memOnServer1 += ct.getMem();

				for (LXC ct : api.getCTs(Constants.SERVER2))
					if (isOurCT(ct))
						memOnServer2 += ct.getMem();

				// Mémoire autorisée sur chaque serveur
				float memRatioOnServer1 = memOnServer1 / memAllowedOnServer1;
				float memRatioOnServer2 = memOnServer2 / memAllowedOnServer2;

				if (memRatioOnServer1 < 1 && memRatioOnServer2 < 1) { // On arrête la génération sur les deux conteneurs
																		// si
																		// l'un d'entre eux dépasse sa RAM autorisée

					// choisir un serveur aléatoirement avec les ratios spécifiés 66% vs 33%
					String serverName;
					if (rndServer.nextFloat() < Constants.CT_CREATION_RATIO_ON_SERVER1)
						serverName = Constants.SERVER1;
					else
						serverName = Constants.SERVER2;

					System.out.print("Création " + (baseCt + ctNum) + " sur le serveur " + serverName + " (vmid "
							+ (baseID + ctNum) + ") ");
					// création container
					if (!api.getCTList(Constants.SERVER1).contains(Long.toString(baseID + ctNum))
							&& !api.getCTList(Constants.SERVER2).contains(Long.toString(baseID + ctNum))) {
						api.createCT(serverName, Long.toString(baseID + ctNum), baseCt + ctNum, Constants.RAM_SIZE[1]);
						System.out.println();
					} else
						System.out.println("...       CT" + (baseID + ctNum) + " existe déjà");

					// planifier la prochaine création
					timeToWait = getNextEventExponential((int) Constants.GENERATION_WAIT_TIME); // par exemple une loi
																								// expo d'une moyenne de
																								// 30sec

				} else {
					System.out.println("Servers are loaded, waiting ...");
					Thread.sleep(Constants.GENERATION_WAIT_TIME * 1000);
				}
			}
			// attendre jusqu'au prochain évènement
			Thread.sleep(1000 * timeToWait);

			synchronized (LOCK) {
				try {
					String srv = getCTServerName(Long.toString(baseID + ctNum), api);
					startAllCTs(api, srv);
				} catch (Exception e) {
				}
			}
			ctNum = (ctNum + 1) % 100;
		}

	}

	public static void startAllCTs(ProxmoxAPI api, String serverName)
			throws LoginException, JSONException, IOException {
		for (LXC ct : api.getCTs(serverName))
			if (isOurCT(ct) && !ct.getStatus().equals("running"))
				api.startCT(serverName, ct.getVmid());
	}

	public static String getCTServerName(String CTvmid, ProxmoxAPI api)
			throws LoginException, JSONException, IOException {
		for (LXC i : api.getCTs(Constants.SERVER1))
			if (i.getVmid().equals(CTvmid))
				return Constants.SERVER1;
		for (LXC i : api.getCTs(Constants.SERVER2))
			if (i.getVmid().equals(CTvmid))
				return Constants.SERVER2;
		return null;
	}

	public static void stopCTs() {
		ProxmoxAPI api = new ProxmoxAPI();
		System.out.println("Arrêt de toutes nos CTs...");

		try {
			String[] servers = { Constants.SERVER1, Constants.SERVER2 };

			for (String serverName : servers) {
				for (LXC ct : api.getCTs(serverName))
					if (isOurCT(ct) && ct.getStatus().equals("running")) {
						System.out.println("Arret CT" + ct.getVmid() + " (" + serverName + ")");
						api.stopCT(serverName, ct.getVmid());
					}
			}

		} catch (LoginException | JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isOurCT(LXC ct) {
		return ct.getName().contains(Constants.CT_BASE_NAME)
				&& Long.parseLong(ct.getVmid()) / 100 == Constants.CT_BASE_ID / 100;
	}

}
