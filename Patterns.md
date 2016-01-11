# Modal Group #

The idea behind modal is to allow only one window and its children to receive user iteraction but without losing the updates from other components.

## Implementation ##

1) Create an interface to define which objects you want to have actions that could only be activated if it object is child of the current modal window.

```
public interface ModalCommand {
	void updateCommands(int elapsedTime);
}
```

2) In your scene hold a stack or the current model object

3) Create a method that goes deep in the current modal object and execute ModalCommand.updateCommand methods.

```
public GameScene extends Scene2D {
	private final LinkedList<Group> modals = new LinkedList<Group>();

	private void updateCommands(Sprite sprite, int elapsedTime) {
		if (sprite instanceof ModalCommand) {
			((ModalCommand) sprite).updateCommands(elapsedTime);
		}

		if (sprite instanceof Group) {
			Iterator<?> i = ((Group) sprite).iterator();
			while (i.hasNext()) {
				updateCommands((Sprite) i.next(), elapsedTime);
			}
		}
	}	

	public void showModal(Group group) {
		modals.add(group);
		add(group);
	}

	public void popModal() {
		remove(modals.removeLast());
	}

	@Override
	public void update(int elapsedTime) {
		super.update(elapsedTime);
		updateCommands(modalPanels.getLast(), elapsedTime);
	}
}
```

4) Implement a class that only has action if it (or their) parent showed as modal

```
public class RotatedSprite extends ImageSprite implements ModalCommand {
	@Override
	public void update(int elapsedTime) {
		angle.set(angle.get() + elapsedTime/1000.0);
	}

	@Override
	public void updateCommands(int elapsedTime) {
		if(isMousePressed()) {
			System.out.println("Clicked");
		}
	}
}
```

# Events Mananger #

Some games have a lot of objects that must be aware of games updates, objects changes and user interactions. There some patterns to solve this problema, for example listeners.

This is a simple solution that solves some problems.

Features:
  * It is assincronous, the event can only be detected in next game time step.
  * There is no need to register or unregister in the event mananger
  * The objects consult the mananger for an event each game step
  * Easy to optimize by making changes only in the mananger

## Implementation ##

```
public class EventsManager {
	/** List of events added in current game step */
	private List<Event> activeEvents = new ArrayList<Event>();
	/** List of events added in previus step */
	private List<Event> events = new ArrayList<Event>();
	/** Optimized list to hold events that are detected to be too much used */
	private List<VeryUsedEvent> veryUsedEvents = new ArrayList<VeryUsedEvent>();
	/** Event called before start to update game step. Its fill the events list and build otimizations */
	public void cleanEvents() {
		events = activeEvents;
		veryUsedEvents.clear();

		for (Event e : events) {
			if (e instanceof VeryUsedEvent) {
				veryUsedEvent.add((VeryUsedEvent) e);
			}
		}

		activeEvents = new ArrayList<Event>();
	}
	/** return all events published last game step */
	public List<Event> findAllEvents() {
		return events;
	}
	/** search events by type, if to much used, hold it in a Map<Class, List<Event>> */
	public <T extends Event> List<T> findEventsByType(Class<T> clazz) {
		List<T> list = new ArrayList<T>();
		for (Event e : list) {
			if (e.getClass() == clazz) {
				list.add((T) e);
			}
		}
		return list;
	}
	/** return otmized list of events */
	public List<VeryUsedEvent> findVeryUsedEvents() {
		return veryUsedEvents;
	}
	/** normal event identification, later if needed it could be otimized to hold in Map<Enemy, List<Event>>  */
	public boolean hasBeenAttacked(Enemy enemy) {
		for (EnemyAttackedEvent e : findEventsByType(PlayerLoseEnergy.class)) {
			if (e.getEnemy() == enemy) {
				return true;
			}
		}
		return false;
	}
	/** normal event identification */
	public boolean hasPlayerLoseEnergy() {
		return !findEventsByType(PlayerLoseEnergy.class).isEmpty();
	}
	/** publish a new event */
	public void publish(Event event) {
		activeEvents.add(event);
	}
}
```