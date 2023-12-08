package hwtracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Scanner;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.temporal.*;
import java.util.function.Consumer;

public class TM {	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("No command found");
			return;
		}
		try {
			Logger.getInstance().getCommandClass(args[0]).execute(args);
		} catch (BadCommandException e) {
			System.out.println("A bad command has been inputted.");
		}
	}
}

class BadCommandException extends Exception {
	BadCommandException() {}
}

class Logger {
	private static Logger logger;
	private static File log;
	private static Map<String, Command> classMap;
	private Logger() {}

	public static Logger getInstance() {
		if (logger == null) {
			logger = new Logger();
			getLog("TaskTrackerLog.txt");
			generateClassMap();
		}
		return logger;
	}

	private static void getLog(String fileName) {
		log = new File(fileName);
		try {
			log.createNewFile();
		} catch (IOException e) {
			System.err.println("A file creation error has occurred.");
		}
	}

	private static void generateClassMap() {
		classMap = new HashMap<>();
		classMap.put("start", new Start());
		classMap.put("stop", new Stop());
		classMap.put("describe", new Describe());
		classMap.put("summary", new Summary());
		classMap.put("size", new Size());
		classMap.put("rename", new Rename());
		classMap.put("delete", new Delete());
	}

	public Command getCommandClass(String command) throws BadCommandException {
		Command commandClass = classMap.get(command);
		if (commandClass == null)
			throw new BadCommandException();
		return commandClass;
	}

	public void writeToFile(String entry) {
		try {
			FileWriter fw = new FileWriter(log, true);
			fw.write(entry);
			fw.close();
		} catch (IOException e) {
			System.err.println("A write error has occurred.");
		}
	}

	public Stream<String> getFileStream() {
		try {
			Stream<String> stream = Files.lines(log.toPath());
			return stream;
		}
		catch (IOException e) {
			System.err.println("Read error");
			return null;
		}
	}
}

interface Command {
	public void execute(String[] args) throws BadCommandException;
}

class Start implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 2 || !canStart())
			throw new BadCommandException();
		String entry = "start " + args[1] + " " + LocalDateTime.now() + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canStart() {
		Map<String, Long> countMap = Logger.getInstance().getFileStream().map(Util::getWords).collect(Collectors.groupingBy(Util::getCommand, Collectors.counting()));
		return (countMap.getOrDefault("start", 0L) <= countMap.getOrDefault("stop", 0L));
	}
}

class Stop implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 2 || !canStop(args[1]))
			throw new BadCommandException();
		String entry = "stop " + args[1] + " " + LocalDateTime.now() + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canStop(String taskName) {
		Map<String, Long> countMap = Logger.getInstance().getFileStream().map(Util::getWords).filter(line -> canStopHelper(line, taskName)).collect(Collectors.groupingBy(Util::getCommand, Collectors.counting()));
		return (countMap.getOrDefault("start", 0L) <= countMap.getOrDefault("stop", 0L));
	}

	private boolean canStopHelper(String[] line, String taskName) {
		if (line[1].equals(taskName) && line[0].equals("start"))
			return true;
		if (line[0].equals("stop"))
			return true;
		return false;
	}
}

class Describe implements Command {
	private String[] sizes = {"S", "M", "L", "XL"};
	private String size = "N";
	
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 3)
			throw new BadCommandException();
		if (args.length > 3)
			if (!Arrays.asList(sizes).contains(args[3]))
				throw new BadCommandException();
			else
				size = args[3];
		String entry = "describe " + args[1] + " " + size + " " + args[2] + "\n";
		Logger.getInstance().writeToFile(entry);
	}
}

class Summary implements Command {
	public class TaskData {
		String size = "Not Set";
		String description = "Not Set";
		int time = 0;
	}

	private String[] sizes = {"S", "M", "L", "XL"};
	private String[] startLine = null;
	private Map<String, TaskData> taskMap;
	private Map<String, Consumer<String[]>> parseMap;

	public void execute(String[] args) throws BadCommandException {
		if (parseMap == null)
			generateParseMap();
		parseLog();
		if (args.length > 1) {
			if (Arrays.asList(sizes).contains(args[1])) {
				System.out.println("Summary for size: " + args[1]);
				String inputSize = args[1];
				taskMap.forEach((task, data) -> {
					if (data.size.equals(inputSize)) {
						printOut(task, data.size, data.description, data.time);
					}
				});
		}
			else {
				System.out.println("Summary for task: " + args[1]);
				String inputTask = args[1];
				taskMap.forEach((task, data) -> {
					if (task.equals(inputTask)) {
						printOut(task, data.size, data.description, data.time);
					}
				});
			}
		}
		else {
			System.out.println("Full Summary");
			taskMap.forEach((task, data) -> {
				printOut(task, data.size, data.description, data.time);
			});
		}
	}
	private void generateParseMap() {
			parseMap = new HashMap<>();
			parseMap.put("start", s -> parseStart(s));
			parseMap.put("stop", s -> parseStop(s));
			parseMap.put("describe", s -> parseDescribe(s));
			parseMap.put("size", s -> parseSize(s));
			parseMap.put("rename", s -> parseRename(s));
			parseMap.put("delete", s -> parseDelete(s));
		}
	
	private void parseLog() {
		taskMap = new HashMap<>();
		Logger.getInstance().getFileStream().map(Util::getWords).forEach(line -> parseMap.get(line[0]).accept(line));
	}

	private void generateTask(String taskName){
		if (!taskMap.containsKey(taskName))
				taskMap.put(taskName, new TaskData());
	}

	private void parseStart(String[] line) {
		if (line.length < 3) {
			System.err.println("Malformed Start");
			return;
		}
		generateTask(line[1]);
		startLine = line;
	}

	private void parseStop(String[] endLine) {
		String startTask = startLine[1], endTask = endLine[1];
		if (startLine.length < 3 || endLine.length < 3 || !startTask.equals(endTask) || !taskMap.containsKey(endTask)) {
			System.err.println("malformed stop");
			return;
		}
		TaskData data = taskMap.get(startTask);
		LocalDateTime startTime = LocalDateTime.parse(startLine[2]), endTime = LocalDateTime.parse(endLine[2]);
		int timeDelta = (int)startTime.until(endTime, ChronoUnit.SECONDS);
		if (timeDelta < 0) {
			System.err.println("Malformed Start/Stop");
			return;
		}
		data.time = timeDelta;
		taskMap.put(startTask, data);
	}

	private void parseDescribe(String[] line) {
		if (line.length < 4) {
			System.err.println("Malformed Describe");
			return;
		}
		String taskName = line[1];
		if (!taskMap.containsKey(taskName))
			generateTask(taskName);
		TaskData data = taskMap.get(taskName);
		parseSize(line);
		data.description = line[3];
		for (String word : Arrays.copyOfRange(line, 4, line.length))
			data.description += " " + word;
		taskMap.put(taskName, data);
	}

	private void parseSize(String[] line) {
		if (line.length < 3){
			System.err.println("Malformed Size");
			return;
		}
		String taskName = line[1];
		if (!taskMap.containsKey(taskName))
			generateTask(taskName);
		TaskData data = taskMap.get(taskName);
		String size = line[2];
		if (Arrays.asList(sizes).contains(size)) {
			data.size = size;
		}
		else if (!size.equals("N"))
			System.err.println("Malformed Size");
		taskMap.put(taskName, data);
	}

	private void parseRename(String[] line) {
		String oldName = line[1], newName = line[2];
		if (line.length < 3 || !taskMap.containsKey(oldName)) {
			System.err.println("Malformed Rename");
			return;
		}
			taskMap.put(newName, taskMap.remove(oldName));
	}

	private void parseDelete(String[] line) {
		if (line.length < 2) {
			System.err.println("Malformed Delete");
			return;
		}
		String taskName = line[1];
		taskMap.remove(taskName);
	}

	private void printOut(String task, String size, String description, int seconds) {
		int[] time = Util.convertSecondsToTime(seconds);
 		System.out.println("Summary for size\t:" + task);
		System.out.println("Size of task\t\t:" + size);
		System.out.println("Description\t\t:" + description);
		System.out.println("Time spent on task\t:" + time[0] + ":" + time[1] + ":" + time[2] + "\n");
	}
}

class Size implements Command {
	String[] sizes = {"S", "M", "L", "XL"};

	public void execute(String[] args) throws BadCommandException {
		if (args.length < 3 || !Arrays.asList(sizes).contains(args[2]))
			throw new BadCommandException();
		else {
			String entry = "size " + args[1] + " " + args[2] + "\n";
			Logger.getInstance().writeToFile(entry);
		}
	}
}

class Rename implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 3)
			throw new BadCommandException();
		String entry = "rename " + args[1] + " " + args[2] + "\n";
		Logger.getInstance().writeToFile(entry);
	}
}

class Delete implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 2 && canDelete())
			throw new BadCommandException();
		String entry = "delete " + args[1] + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canDelete() {
		Map<String, Long> countMap = Logger.getInstance().getFileStream().map(Util::getWords).collect(Collectors.groupingBy(Util::getCommand, Collectors.counting()));
		return (countMap.getOrDefault("start", 0L) <= countMap.getOrDefault("stop", 0L));
	}
}

class Util {
	public static String[] getWords(String string) {
		return string.split("\\s+");
	}

	public static int[] convertSecondsToTime(int seconds) {
		int hours = seconds / 3600;
		int minutes = seconds % 3600 / 60;
		seconds = seconds % 60;
		return new int[] { hours, minutes, seconds };
	}

	public static String getCommand(String[] line) {
		return line[0];
	}
}