package hwtracker;

import java.io.File;
import java.io.IOException;

import java.time.LocalTime; 

public class TM {

	public static void main(String[] args) {

		if (args.length < 2) {
			System.out.println("No command found");
			return;
		}
		
		File tracker = new File("TaskTracker.txt");

		try {
			if (tracker.createNewFile() == true)
				System.out.println("File created");
			else
				System.out.println("File already exists");
		} catch (IOException e) {
			System.out.println("An error occurred.");
      		e.printStackTrace();
		}

		if (args[2].equals("start")) {
			start(tracker);
		}
		else if (args[2].equals("stop")) {
			stop(tracker);
		}
		else if (args[2].equals("describe")) {
			describe(tracker);
		}
		else if (args[2].equals("size")) {
			size(tracker);
		}
		else if (args[2].equals("rename")) {
			rename(tracker);
		}
		else if (args[2].equals("summary")) {
			summary(tracker);
		}

	}

	private static void start(File tracker) {

	}

	private static void stop(File tracker) {

	}

	private static void describe(File tracker) {

	}

	private static void size(File tracker) {

	}

	private static void rename(File tracker) {

	}

	private static void summary(File tracker) {

	}

}
