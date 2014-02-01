/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.AI;

import net.windward.Windwardopolis2.api.*;

import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * The sample C# AI. Start with this project but write your own code as this is
 * a very simplistic implementation of the AI.
 */
public class MyPlayerBrain implements net.windward.Windwardopolis2.AI.IPlayerAI {

	private static String NAME = "DROP_TABLES";

	public static String SCHOOL = "Purdue U.";

	private static Logger log = Logger.getLogger(IPlayerAI.class);

	private static PassengerComparator passengerComparator;

	private static CoffeeStoreComparator coffeeStoreComparator;

	private static CompanyComparator companyComparator;

	/**
	 * The name of the player.
	 */
	private String privateName;

	public final String getName() {
		return privateName;
	}

	private void setName(String value) {
		privateName = value;
	}

	/**
	 * The game map.
	 */
	private Map privateGameMap;

	public final Map getGameMap() {
		return privateGameMap;
	}

	private void setGameMap(Map value) {
		privateGameMap = value;
	}

	/**
	 * All of the players, including myself.
	 */
	private java.util.ArrayList<Player> privatePlayers;

	public final java.util.ArrayList<Player> getPlayers() {
		return privatePlayers;
	}

	private void setPlayers(java.util.ArrayList<Player> value) {
		privatePlayers = value;
	}

	/**
	 * All of the companies.
	 */
	private java.util.ArrayList<Company> privateCompanies;

	public final java.util.ArrayList<Company> getCompanies() {
		Collections.sort(privateCompanies, companyComparator);
		return privateCompanies;
	}

	private void setCompanies(java.util.ArrayList<Company> value) {
		privateCompanies = value;
	}

	/**
	 * All of the passengers.
	 */
	private java.util.ArrayList<Passenger> privatePassengers;

	public final java.util.ArrayList<Passenger> getPassengers() {
		return privatePassengers;
	}

	private void setPassengers(java.util.ArrayList<Passenger> value) {
		privatePassengers = value;
	}

	/**
	 * All of the coffee stores.
	 */
	private java.util.ArrayList<CoffeeStore> privateStores;

	public final ArrayList<CoffeeStore> getCoffeeStores() {
		Collections.sort(privateStores, coffeeStoreComparator);
		return privateStores;
	}

	private void setCoffeeStores(ArrayList<CoffeeStore> value) {
		privateStores = value;
	}

	/**
	 * The power up deck
	 */
	private ArrayList<PowerUp> privatePowerUpDeck;

	public final ArrayList<PowerUp> getPowerUpDeck() {
		return privatePowerUpDeck;
	}

	private void setPowerUpDeck(ArrayList<PowerUp> value) {
		privatePowerUpDeck = value;
	}

	/**
	 * My power up hand
	 */
	private ArrayList<PowerUp> privatePowerUpHand;

	public final ArrayList<PowerUp> getPowerUpHand() {
		return privatePowerUpHand;
	}

	private void setPowerUpHand(ArrayList<PowerUp> value) {
		privatePowerUpHand = value;
	}

	/**
	 * Me (my player object).
	 */
	private Player privateMe;

	public final Player getMe() {
		return privateMe;
	}

	private void setMe(Player value) {
		privateMe = value;
	}

	/**
	 * My current passenger
	 */
	private Passenger privateMyPassenger;

	public final Passenger getMyPassenger() {
		return privateMyPassenger;
	}

	private void setMyPassenger(Passenger value) {
		privateMyPassenger = value;
	}

	private PlayerAIBase.PlayerOrdersEvent sendOrders;

	private PlayerAIBase.PlayerCardEvent playCards;

	/**
	 * The maximum number of trips allowed before a refill is required.
	 */
	private static final int MAX_TRIPS_BEFORE_REFILL = 3;

	private static final java.util.Random rand = new java.util.Random();

	public MyPlayerBrain(String name) {
		setName(!net.windward.Windwardopolis2.DotNetToJavaStringHelper
				.isNullOrEmpty(name) ? name : NAME);
		privatePowerUpHand = new ArrayList<PowerUp>();
	}

	/**
	 * The avatar of the player. Must be 32 x 32.
	 */
	public final byte[] getAvatar() {
		try {
			// open image
			InputStream stream = getClass().getResourceAsStream(
					"/net/windward/Windwardopolis2/res/MyAvatar.png");

			byte[] avatar = new byte[stream.available()];
			stream.read(avatar, 0, avatar.length);
			return avatar;

		} catch (IOException e) {
			System.out.println("error reading image");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Called at the start of the game.
	 * 
	 * @param map
	 *            The game map.
	 * @param me
	 *            You. This is also in the players list.
	 * @param players
	 *            All players (including you).
	 * @param companies
	 *            The companies on the map.
	 * @param passengers
	 *            The passengers that need a lift.
	 * @param ordersEvent
	 *            Method to call to send orders to the server.
	 */
	public final void Setup(Map map, Player me,
			java.util.ArrayList<Player> players,
			java.util.ArrayList<Company> companies,
			ArrayList<CoffeeStore> stores,
			java.util.ArrayList<Passenger> passengers,
			ArrayList<PowerUp> powerUps,
			PlayerAIBase.PlayerOrdersEvent ordersEvent,
			PlayerAIBase.PlayerCardEvent cardEvent) {

		try {
			passengerComparator = new PassengerComparator(this);
			coffeeStoreComparator = new CoffeeStoreComparator(this);
			companyComparator = new CompanyComparator(this);
			setGameMap(map);
			setPlayers(players);
			setMe(me);
			setCompanies(companies);
			setPassengers(passengers);
			setCoffeeStores(stores);
			setPowerUpDeck(powerUps);
			sendOrders = ordersEvent;
			playCards = cardEvent;

			java.util.ArrayList<Passenger> pickup = AllPickups(me, passengers);

			// get the path from where we are to the dest.
			java.util.ArrayList<Point> path = CalculatePathPlus1(me, pickup
					.get(0).getLobby().getBusStop());
			sendOrders.invoke("ready", path, pickup);
		} catch (RuntimeException ex) {
			log.fatal("setup(" + me == null ? "NULL" : me.getName()
					+ ") Exception: " + ex.getMessage());
			ex.printStackTrace();

		}
	}

	/**
	 * Called to send an update message to this A.I. We do NOT have to send
	 * orders in response.
	 * 
	 * @param status
	 *            The status message.
	 * @param plyrStatus
	 *            The player this status is about. THIS MAY NOT BE YOU.
	 */
	// public final void GameStatus(PlayerAIBase.STATUS status, Player
	// plyrStatus) {
	// // bugbug - Framework.cs updates the object's in this object's Players,
	// // Passengers, and Companies lists. This works fine as long
	// // as this app is single threaded. However, if you create worker
	// // thread(s) or respond to multiple status messages simultaneously
	// // then you need to split these out and synchronize access to the saved
	// // list objects.
	// try {
	// // bugbug - we return if not us because the below code is only for
	// // when we need a new path or our limo hit a bus stop.
	// // if you want to act on other players arriving at bus stops, you
	// // need to remove this. But make sure you use Me, not
	// // plyrStatus for the Player you are updatiing (particularly to
	// // determine what tile to start your path from).
	// if (plyrStatus != getMe()) {
	// return;
	// }
	//
	// if (status == PlayerAIBase.STATUS.UPDATE) {
	// MaybePlayPowerUp();
	// return;
	// }
	//
	// DisplayStatus(status, plyrStatus);
	//
	// if (log.isDebugEnabled())
	// log.info("gameStatus( " + status + " )");
	//
	// Point ptDest = null;
	// java.util.ArrayList<Passenger> pickup = new
	// java.util.ArrayList<Passenger>();
	// switch (status) {
	// case NO_PATH:
	// case PASSENGER_NO_ACTION:
	// if (getMe().getLimo().getPassenger() == null) {
	// pickup = AllPickups(plyrStatus, getPassengers());
	// ptDest = pickup.get(0).getLobby().getBusStop();
	// } else {
	// ptDest = getMe().getLimo().getPassenger().getDestination()
	// .getBusStop();
	// }
	// break;
	// case PASSENGER_DELIVERED:
	// case PASSENGER_ABANDONED:
	// pickup = AllPickups(getMe(), getPassengers());
	// ptDest = pickup.get(0).getLobby().getBusStop();
	// break;
	// case PASSENGER_REFUSED_ENEMY:
	// java.util.List<Company> comps = getCompanies();
	// int i=0;
	// while (ptDest == null) {
	// if (comps.get(i) != getMe().getLimo()
	// .getPassenger().getDestination()) {
	// ptDest = getCompanies().get(i).getBusStop();
	// break;
	// }
	// i++;
	// }
	// break;
	// case PASSENGER_DELIVERED_AND_PICKED_UP:
	// case PASSENGER_PICKED_UP:
	// pickup = AllPickups(getMe(), getPassengers());
	// ptDest = getMe().getLimo().getPassenger().getDestination()
	// .getBusStop();
	// break;
	//
	// }
	//
	// // coffee store override
	// switch (status) {
	// case PASSENGER_DELIVERED_AND_PICKED_UP:
	// case PASSENGER_DELIVERED:
	// case PASSENGER_ABANDONED:
	// if (getMe().getLimo().getCoffeeServings() <= 0) {
	// ptDest = getCoffeeStores().get(0).getBusStop();
	// }
	// break;
	// case PASSENGER_REFUSED_NO_COFFEE:
	// case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
	// ptDest = getCoffeeStores().get(0).getBusStop();
	// break;
	// case COFFEE_STORE_CAR_RESTOCKED:
	// pickup = AllPickups(getMe(), getPassengers());
	// if (pickup.size() == 0)
	// break;
	// ptDest = pickup.get(0).getLobby().getBusStop();
	// break;
	// }
	//
	// // may be another status
	// if (ptDest == null)
	// return;
	//
	// DisplayOrders(ptDest);
	//
	// // get the path from where we are to the dest.
	// java.util.ArrayList<Point> path = CalculatePathPlus1(getMe(),
	// ptDest);
	//
	// if (log.isDebugEnabled()) {
	// log.debug(status
	// + "; Path:"
	// + (path.size() > 0 ? path.get(0).toString() : "{n/a}")
	// + "-"
	// + (path.size() > 0 ? path.get(path.size() - 1)
	// .toString() : "{n/a}")
	// + ", "
	// + path.size()
	// + " steps; Pickup:"
	// + (pickup.size() == 0 ? "{none}" : pickup.get(0)
	// .getName()) + ", " + pickup.size() + " total");
	// }
	//
	// // update our saved Player to match new settings
	// if (path.size() > 0) {
	// getMe().getLimo().getPath().clear();
	// getMe().getLimo().getPath().addAll(path);
	// }
	// if (pickup.size() > 0) {
	// getMe().getPickUp().clear();
	// getMe().getPickUp().addAll(pickup);
	// }
	//
	// sendOrders.invoke("move", path, pickup);
	// } catch (RuntimeException ex) {
	// ex.printStackTrace();
	// }
	// }

	public final void GameStatus(PlayerAIBase.STATUS status, Player plyrStatus) {
		for (Iterator i = AllPickups(getMe(), getPassengers()).iterator(); i.hasNext();) {
			Passenger p = (Passenger) i.next();
			//System.err.println(p + " " + getScore(p));
		}
		
		Point ptDest = null;
		ArrayList<Passenger> pickup = new ArrayList<Passenger>();
		if (getMe().getLimo().getCoffeeServings() <= 0) {
			ptDest = getCoffeeStores().get(0).getBusStop();
		} else {
			ArrayList<Passenger> poss = AllPickups(getMe(), getPassengers());
			if (getMe().getLimo().getPassenger() != null)
				poss.add(getMe().getLimo().getPassenger());
			double max = Double.NEGATIVE_INFINITY;
			Passenger toPickup = null;
			for (Passenger p : poss) {
				if (getScore(p) > max) {
					max = getScore(p);
					toPickup = p;
				}
			}
			ptDest = getMe().getLimo().getPassenger() == toPickup ? toPickup
					.getDestination().getBusStop() : toPickup.getLobby()
					.getBusStop();
			pickup.add(toPickup);
		}

//		DisplayOrders(ptDest);

		// get the path from where we are to the dest.
		ArrayList<Point> path = CalculatePathPlus1(getMe(), ptDest);

		if (log.isDebugEnabled()) {
			log.debug(status
					+ "; Path:"
					+ (path.size() > 0 ? path.get(0).toString() : "{n/a}")
					+ "-"
					+ (path.size() > 0 ? path.get(path.size() - 1).toString()
							: "{n/a}") + ", " + path.size() + " steps; Pickup:"
					+ (pickup.size() == 0 ? "{none}" : pickup.get(0).getName())
					+ ", " + pickup.size() + " total");
		}
		getMe().getLimo().getPath().clear();
		getMe().getLimo().getPath().addAll(path);
		getMe().getPickUp().clear();
		getMe().getPickUp().addAll(pickup);
		sendOrders.invoke("move", path, pickup);
	}

	private void MaybePlayPowerUp() {
		if ((getPowerUpHand().size() != 0) && (rand.nextInt(50) < 30))
			return;
		// not enough, draw
		if (getPowerUpHand().size() < getMe().getMaxCardsInHand()
				&& getPowerUpDeck().size() > 0) {
			for (int index = 0; index < getMe().getMaxCardsInHand()
					- getPowerUpHand().size()
					&& getPowerUpDeck().size() > 0; index++) {
				// select a card
				PowerUp pu = getPowerUpDeck().get(0);
				privatePowerUpDeck.remove(pu);
				privatePowerUpHand.add(pu);
				playCards.invoke(PlayerAIBase.CARD_ACTION.DRAW, pu);
			}
			return;
		}

		// can we play one?
		PowerUp pu2 = null;
		for (PowerUp current : getPowerUpHand()) {
			if (current.isOkToPlay()) {
				pu2 = current;
				break;
			}
		}

		if (pu2 == null)
			return;
		// 10% discard, 90% play
		if (rand.nextInt(10) == 0) {
			playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, pu2);
		} else {
			if (pu2.getCard() == PowerUp.CARD.MOVE_PASSENGER) {
				Passenger toUseCardOn = null;
				for (Passenger pass : privatePassengers) {
					if (pass.getCar() == null) {
						toUseCardOn = pass;
						break;
					}
				}
				pu2.setPassenger(toUseCardOn);
			}
			if (pu2.getCard() == PowerUp.CARD.CHANGE_DESTINATION
					|| pu2.getCard() == PowerUp.CARD.STOP_CAR) {
				java.util.ArrayList<Player> plyrsWithPsngrs = new ArrayList<Player>();
				for (Player play : privatePlayers) {
					if (play.getGuid() != getMe().getGuid()
							&& play.getLimo().getPassenger() != null) {
						plyrsWithPsngrs.add(play);
					}
				}

				if (plyrsWithPsngrs.size() == 0)
					return;
				// target highest scoring player in current game
				Player targetPlyr = plyrsWithPsngrs.get(0);
				for (Player plyr : plyrsWithPsngrs) {
					if (plyr.getScore() > targetPlyr.getScore())
						targetPlyr = plyr;
				}
				pu2.setPlayer(targetPlyr);
			}
			if (log.isInfoEnabled())
				log.info("Request play card " + pu2);
			playCards.invoke(PlayerAIBase.CARD_ACTION.PLAY, pu2);
		}
		privatePowerUpHand.remove(pu2);
	}

	/**
	 * A power-up was played. It may be an error message, or success.
	 * 
	 * @param puStatus
	 *            - The status of the played card.
	 * @param plyrPowerUp
	 *            - The player who played the card.
	 * @param cardPlayed
	 *            - The card played.
	 */
	public void PowerupStatus(PlayerAIBase.STATUS puStatus, Player plyrPowerUp,
			PowerUp cardPlayed) {
		// redo the path if we got relocated
		if ((puStatus == PlayerAIBase.STATUS.POWER_UP_PLAYED)
				&& ((cardPlayed.getCard() == PowerUp.CARD.RELOCATE_ALL_CARS) || ((cardPlayed
						.getCard() == PowerUp.CARD.CHANGE_DESTINATION) && (cardPlayed
						.getPlayer() != null ? cardPlayed.getPlayer().getGuid()
						: null) == getMe().getGuid())))
			GameStatus(PlayerAIBase.STATUS.NO_PATH, getMe());
	}

	private void DisplayStatus(PlayerAIBase.STATUS status, Player plyrStatus) {
		String msg = null;
		switch (status) {
		case PASSENGER_DELIVERED:
			msg = getMyPassenger().getName() + " delivered to "
					+ getMyPassenger().getLobby().getName();
			privateMyPassenger = null;
			break;
		case PASSENGER_ABANDONED:
			msg = getMyPassenger().getName() + " abandoned at "
					+ getMyPassenger().getLobby().getName();
			privateMyPassenger = null;
			break;
		case PASSENGER_REFUSED_ENEMY:
			msg = plyrStatus.getLimo().getPassenger().getName()
					+ " refused to exit at "
					+ plyrStatus.getLimo().getPassenger().getDestination()
							.getName() + " - enemy there";
			break;
		case PASSENGER_DELIVERED_AND_PICKED_UP:
			msg = getMyPassenger().getName() + " delivered at "
					+ getMyPassenger().getLobby().getName() + " and "
					+ plyrStatus.getLimo().getPassenger().getName()
					+ " picked up";
			privateMyPassenger = plyrStatus.getLimo().getPassenger();
			break;
		case PASSENGER_PICKED_UP:
			msg = plyrStatus.getLimo().getPassenger().getName() + " picked up";
			privateMyPassenger = plyrStatus.getLimo().getPassenger();
			break;
		case PASSENGER_REFUSED_NO_COFFEE:
			msg = "Passenger refused to board limo, no coffee";
			break;
		case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
			msg = getMyPassenger().getName() + " delivered at "
					+ getMyPassenger().getLobby().getName()
					+ ", new passenger refused to board limo, no coffee";
			break;
		case COFFEE_STORE_CAR_RESTOCKED:
			msg = "Coffee restocked!";
			break;
		}
		if (msg != null && !msg.equals("")) {
			System.out.println(msg);
			if (log.isInfoEnabled())
				log.info(msg);
		}
	}

	private void DisplayOrders(Point ptDest) {
		String msg = null;
		CoffeeStore store = null;
		for (CoffeeStore s : getCoffeeStores()) {
			if (s.getBusStop() == ptDest) {
				store = s;
				break;
			}
		}

		if (store != null)
			msg = "Heading toward " + store.getName() + " at "
					+ ptDest.toString();
		else {
			Company company = null;
			for (Company c : getCompanies()) {
				if (c.getBusStop() == ptDest) {
					company = c;
					break;
				}
			}

			if (company != null)
				msg = "Heading toward " + company.getName() + " at "
						+ ptDest.toString();
		}
		if (msg != null && !msg.equals("")) {
			System.out.println(msg);
			if (log.isInfoEnabled())
				log.info(msg);
		}
	}

	private java.util.ArrayList<Point> CalculatePathPlus1(Player me,
			Point ptDest) {
		java.util.ArrayList<Point> path = SimpleAStar.CalculatePath(
				getGameMap(), me.getLimo().getMapPosition(), ptDest);
		// add in leaving the bus stop so it has orders while we get the message
		// saying it got there and are deciding what to do next.
		if (path.size() > 1) {
			path.add(path.get(path.size() - 2));
		}
		return path;
	}

	// returns a list of possible passengers to pickup
	private static java.util.ArrayList<Passenger> AllPickups(Player me,
			Iterable<Passenger> passengers) {
		java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();

		for (Passenger psngr : passengers) {
			if ((!me.getPassengersDelivered().contains(psngr))
					&& (psngr != me.getLimo().getPassenger())
					&& (psngr.getCar() == null) && (psngr.getLobby() != null)
					&& (psngr.getDestination() != null))
				pickup.add(psngr);
		}
		Collections.sort(pickup, passengerComparator);
		return pickup;
	}

	private double getScore(Passenger pass) {
		// Calculates total distance to completion over
		Point source = pass == getMe().getLimo().getPassenger() ? getMe()
				.getLimo().getMapPosition() : pass.getLobby().getBusStop();
		int distToDest = SimpleAStar.CalculatePath(getGameMap(), source,
				pass.getDestination().getBusStop()).size();
		int distToPass = CalculatePathPlus1(privateMe, source).size();
		double score = (double) pass.getPointsDelivered() / (distToDest + distToPass);
		
		double enemyMultiplier;
		if(Collections.disjoint(pass.getDestination().getPassengers(), pass.getEnemies())) {
			enemyMultiplier = 1.0;
		} else {
			double m=1000;
			enemyMultiplier = -(m / (distToDest+m))+1;
			enemyMultiplier = 0.0;
		}
		
		score *= enemyMultiplier;
		
		return score;
	}

	private static class PassengerComparator implements Comparator<Passenger> {
		MyPlayerBrain brain;

		// compares passengers based on their total travel distance and point
		// value
		public int compare(Passenger a, Passenger b) {
			double aScore = brain.getScore(a);
			double bScore = brain.getScore(b);
			if (aScore - bScore < 0) {
				return -1;
			} else if (aScore -bScore > 0) {
				return 1;
			} else {
				return 0;
			}
		}

		public PassengerComparator(MyPlayerBrain brain) {
			this.brain = brain;
		}
	}

	private static class CoffeeStoreComparator implements
			Comparator<CoffeeStore> {
		MyPlayerBrain brain;

		public int compare(CoffeeStore a, CoffeeStore b) {
			int aDist = 0, bDist = 0;
			aDist = brain.CalculatePathPlus1(brain.privateMe, a.getBusStop())
					.size();
			bDist = brain.CalculatePathPlus1(brain.privateMe, b.getBusStop())
					.size();
			return (aDist <= bDist ? -1 : 1);
		}

		public CoffeeStoreComparator(MyPlayerBrain brain) {
			this.brain = brain;
		}
	}

	private static class CompanyComparator implements Comparator<Company> {
		MyPlayerBrain brain;

		// sorts based on the distance between us and the companies' bus stops
		public int compare(Company a, Company b) {
			int aDist = 0, bDist = 0;
			aDist = brain.CalculatePathPlus1(brain.privateMe, a.getBusStop())
					.size();
			bDist = brain.CalculatePathPlus1(brain.privateMe, b.getBusStop())
					.size();
			return (aDist <= bDist ? -1 : 1);
		}

		public CompanyComparator(MyPlayerBrain brain) {
			this.brain = brain;
		}
	}
}