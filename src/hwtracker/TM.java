package hwtracker;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.io.IOException;
import java.util.Arrays;

import java.time.LocalTime; 

public class TM {
	static class BadCommandException extends Exception {
		BadCommandException() {}
	}

	public interface Command {
		public void action(String[] args, File log) throws BadCommandException;
	}

	static public class Start implements Command{
		public void action(String[] args, File log) throws BadCommandException {
			if (args.length < 2 || !canStart(log))
				throw new BadCommandException();
			String entry = args[1] + " started " + LocalTime.now() + "\n";
			writeToFile(entry, log);
		}

		private boolean canStart(File log) {
			int start = 0, stop = 0, i = 0;
			String line;
			try {
				Scanner scanner = new Scanner(log);
				while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					if (line.contains("started"))
						start = i;
					if (line.contains("stopped"))
						stop = i;
					i++;
				}
				scanner.close();
			} catch (Exception e){
				System.out.println("A read error has occurred.");
			}
			return (start < stop);
		}
	}

	static public class Stop implements Command{
		public void action(String[] args, File log) throws BadCommandException {
			if (args.length < 2 || !canStop(log, args[1]))
				throw new BadCommandException();
			String entry = args[1] + " stopped " + LocalTime.now() + "\n";
			writeToFile(entry, log);
		}

		private boolean canStop(File log, String taskName) {
			int start = 0, stop = 0, i = 0;
			String line;
			try {
				Scanner scanner = new Scanner(log);
				while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					if (line.contains("started") && line.contains(taskName))
						start = i;
					if (line.contains("stopped"))
						stop = i;
					i++;
				}
				scanner.close();
			} catch (Exception e){
				System.out.println("A read error has occurred.");
			}
			return (stop < start);
		}
	}

	static public class Describe implements Command{
		private String[] sizes = {"S", "M", "L", "XL"};
		private String size = "N";
		private int startIndex = 2;
		private String entry;

		public void action(String[] args, File log) throws BadCommandException {
			if (args.length < 3)
				throw new BadCommandException();
			getSize(args[2]);
			createEntry(args);
			writeToFile(entry, log);
		}

		private void getSize(String input) {
			if (Arrays.asList(sizes).contains(input)) {
				size = input;
				startIndex = 3;
			}
		}

		private void createEntry(String[] args) {
			entry = args[1] + " described " + size;
			for (int i = startIndex; i < args.length; i++)
				entry += " " + args[i];
			entry += "\n";
		}
	}

	static public class Summary implements Command{
		String[] sizes = {"S", "M", "L", "XL"};
		public void action(String[] args, File log) throws BadCommandException {
			if (args.length > 1) {
				if (Arrays.asList(sizes).contains(args[2])) {
					System.out.println("Size " + args[2] + " Summary");
				}
				else {
					System.out.println("Task " + args[2] + " Summary");
				}
			}
			else {
				System.out.println("Full Summary");
			}
		}
	}

	static public class Size implements Command{
		String[] sizes = {"S", "M", "L", "XL"};
		public void action(String[] args, File log) throws BadCommandException {
			if (args.length < 3 || !Arrays.asList(sizes).contains(args[2]))
				throw new BadCommandException();
			else {
				String entry = args[1] + " size " + args[2] + "\n";
				writeToFile(entry, log);
			}
		}
	}

	static public class Rename implements Command{
		public void action(String[] args, File log) throws BadCommandException {
			if (args.length < 3)
				throw new BadCommandException();
			String entry = args[1] + " renamed " + args[2] + "\n";
			writeToFile(entry, log);
		}
	}

	static public class Delete implements Command{
		public void action(String[] args, File log) throws BadCommandException {
			if (args.length < 2)
				throw new BadCommandException();
			String entry = args[1] + " deleted" + "\n";
			writeToFile(entry, log);
		}
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("No command found");
			return;
		}
		
		File trackerLog = getLog("TasktrackerLog.txt");
		Command cc = getCommandClass(args[0]);
		try {
			cc.action(args, trackerLog);
		} catch (Exception e) {
			System.out.println("A bad command has been inputted.");
		}
	}

	public static File getLog(String fileName) {
		File log = new File(fileName);
		try {
			log.createNewFile();
		} catch (IOException e) {
			System.out.println("A file creation error has occurred.");
		}
		return log;
	}

	public static Command getCommandClass(String command) {
		switch (command) {
			case "start": return new Start();
			case "stop": return new Stop();
			case "describe": return new Describe();
			case "summary": return new Summary();
			case "size": return new Size();
			case "rename": return new Rename();
			case "delete": return new Delete();
			default:
				return null;
		}
	}

	public static void writeToFile(String entry, File log) {
		try {
			FileWriter fw = new FileWriter(log, true);
			fw.write(entry);
			fw.close();
		} catch (IOException e) {
			System.out.println("A write error has occurred.");
		}
	}
}