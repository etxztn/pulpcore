# Most Wanted #

Below there is a list of some components that we would like to have in this section.

  * Layouts: FlowLayout, GridLayout, BorderLayout, etc...
  * GUI components: Edit, selects, combos
  * Scene change effects

# Component: Timer #

This simple class is used to verify when some time passed. It is very useful to execute some actions from time to time.

```
public class Timer {
	private int time;
	private int delay;
	public Timer() {
		this(0);
	}
	public Timer(int delay) {
		this.setDelay(delay);
	}
	public void forceActivate() {
		time = 0;
	}
	/**
	 * @return time needed to activate the timer
	 */
	public int getCurrent() {
		return time;
	}
	public int getDelay() {
		return delay;
	}
	public boolean isActive() {
		return time <= 0;
	}
	public boolean verify(int elapsedTime) {
		update(elapsedTime);
		if (isActive()) {
			reset();
			return true;
		} else {
			return false;
		}
	}
	public void reset() {
		time = delay;
	}
	public void setDelay(int delay) {
		this.delay = delay;
		reset();
	}
	public void update(int elapsedTime) {
		time -= elapsedTime;
	}
}
```

Below there is a example in how to use it. It add a new sprite each 2 seconds.
```
public class GroupEntitySprite extends Group {
	private Timer timer = new Timer(2000);
	public void update(int elapsedTime) {
		super.update(elapsedTime);
		if(timer.verify(elapsedTime)) {
			add(new ImageSprite("image.png", 0,0));
		}
	}
}
```