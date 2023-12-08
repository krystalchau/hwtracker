package hwtracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;
import java.io.IOException;
import java.util.Arrays;

import java.time.LocalTime; 

public class TM {	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("No command found");
			return;
		}
		try {
			Command cc = Logger.getInstance().getCommandClass(args[0]);
			cc.action(args);
		} catch (BadCommandException e) {
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
}

class BadCommandException extends Exception {
	BadCommandException() {}
}

class Logger {
	private static Logger logger;
	private static File log;
	private Logger() {}


	private static void getLog(String fileName) {
		log = new File(fileName);
		try {
			log.createNewFile();
		} catch (IOException e) {
			System.out.println("A file creation error has occurred.");
		}
	}

	public static Logger getInstance() {
		if (logger == null) {
			logger = new Logger();
			getLog("TaskTrackerLog.txt");
		}
		return logger;
	}


	public Command getCommandClass(String command) throws BadCommandException{
		switch (command) {
			case "start": return new Start();
			case "stop": return new Stop();
			case "describe": return new Describe();
			case "summary": return new Summary();
			case "size": return new Size();
			case "rename": return new Rename();
			case "delete": return new Delete();
			default:
				throw new BadCommandException();
		}
	}
	public void writeToFile(String entry) {
		try {
			FileWriter fw = new FileWriter(log, true);
			fw.write(entry);
			fw.close();
		} catch (IOException e) {
			System.out.println("A write error has occurred.");
		}
	}

	public Scanner getFileReader() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(log);
		} catch (FileNotFoundException e){
			System.out.println("A read error has occurred.");
		}
		return scanner;
	}
}

interface Command {
	public void action(String[] args) throws BadCommandException; 
}

 class Start implements Command{
		public void action(String[] args) throws BadCommandException {
			if (args.length < 2 || !canStart())
				throw new BadCommandException();
			String entry = "start " + args[1] + " " + LocalTime.now() + "\n";
			Logger.getInstance().writeToFile(entry);
		}

		private boolean canStart() {
			int start = -1, stop = 0, i = 0;
			String line;
			Scanner scanner = Logger.getInstance().getFileReader();
			while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					if (line.contains("start"))
						start = i;
					if (line.contains("stop"))
						stop = i;
					i++;
				}
				scanner.close();
			return (start < stop);
		}
	}

class Stop implements Command{
	public void action(String[] args) throws BadCommandException {
		if (args.length < 2 || !canStop(args[1]))
			throw new BadCommandException();
		String entry = "stop " + args[1] + " " + LocalTime.now() + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canStop(String taskName) {
		int start = 0, stop = -1, i = 0;
		String line;
		Scanner scanner = Logger.getInstance().getFileReader();
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line.contains(taskName) && line.contains("start"))
				start = i;
			if (line.contains("stop"))
				stop = i;
			i++;
		}
		scanner.close();
		return (stop < start);
	}
}

class Describe implements Command{
	private String[] sizes = {"S", "M", "L", "XL"};
	private String size = "N";
	public void action(String[] args) throws BadCommandException {
		if (args.length < 3)
			throw new BadCommandException();
		if (args.length > 3)
			if (!Arrays.asList(sizes).contains(args[3]))
				throw new BadCommandException();
			else
				size = args[3];
		String entry = "describe " + args[1] + " " + args[2] + " " + size + "\n";
		Logger.getInstance().writeToFile(entry);
	}
}

class Summary implements Command{
	String[] sizes = {"S", "M", "L", "XL"};
	public void action(String[] args) throws BadCommandException {
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

class Size implements Command{
	String[] sizes = {"S", "M", "L", "XL"};
	public void action(String[] args) throws BadCommandException {
		if (args.length < 3 || !Arrays.asList(sizes).contains(args[2]))
			throw new BadCommandException();
		else {
			String entry = "size " + args[1] + " " + args[2] + "\n";
			Logger.getInstance().writeToFile(entry);
		}
	}
}

class Rename implements Command{
	public void action(String[] args) throws BadCommandException {
		if (args.length < 3)
			throw new BadCommandException();
		String entry = "rename " + args[1] + " " + args[2] + "\n";
		Logger.getInstance().writeToFile(entry);
	}
}

class Delete implements Command{
	public void action(String[] args) throws BadCommandException {
		if (args.length < 2)
			throw new BadCommandException();
		String entry = "delete " + args[1] + "\n";
		Logger.getInstance().writeToFile(entry);
	}
}

class Util {
	public static String[] getWords(String string) {
		return string.split("\\s+");
	}
}