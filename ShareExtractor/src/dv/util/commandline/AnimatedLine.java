package dv.util.commandline;

public class AnimatedLine implements Runnable {

	private char[] chars = { '|', '/', '-', '\\', '|', '/', '-' };
	private boolean run = true;
	private String string;
	private static AnimatedLine animation;
	
	private AnimatedLine(String string) {
		this.string = string;
	}

	/**
	 * prints out a spinning dash to stdout and returns immediately
	 * @param string
	 */
	public static void startThinking(String string) {
		if (animation != null) {
			stopThinking();
		}
		animation = new AnimatedLine(string);
		new Thread(animation).start();
	}

	public static void stopThinking() {
		if (animation != null) {
			animation.run = false;
			animation = null;
			System.out.println();
		}
	}

	@Override
	public void run() {
		while (run)
			for (char c : chars) {
				if (run) {
					System.out.print(string + " " + c);
					try {
						if (run)
							Thread.sleep(200);
					} catch (InterruptedException e) {
						// w/e who cares
					}
					if (run)
						System.out.print('\r');
				}
			}
	}
}
