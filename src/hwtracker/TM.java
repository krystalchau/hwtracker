package hwtracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;

import java.time.LocalDateTime;
import java.time.temporal.*;
import java.time.format.DateTimeParseException;

import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
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
			System.out.println("A file creation error has occurred.");
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
			System.out.println("A write error has occurred.");
		}
	}

	public Scanner getFileReader() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(log);
		} catch (FileNotFoundException e) {
			System.out.println("A read error has occurred.");
		}
		return scanner;
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
		if (args.length < 2 || !canStart() 
				|| Arrays.asList(Util.getSizes()).contains(args[1]))
			throw new BadCommandException();
		String entry = "start " + args[1] + " " + LocalDateTime.now() + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canStart() {
		return Util.startBeforeStop();
	}
}

class Stop implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 2 || canStop(args[1]))
			throw new BadCommandException();
		String entry = "stop " + args[1] + " " + LocalDateTime.now() + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canStop(String taskName) {
		int start = 0, stop = -1, i = 0;
		Scanner scanner = Logger.getInstance().getFileReader();
		while (scanner.hasNextLine()) {
			String[] line = Util.getWords(scanner.nextLine());
			if (line[0].equals("start") && line[1].equals(taskName))
				start = i;
			if (line[0].equals("stop"))
				stop = i;
			i++;
		}
		scanner.close();
		return (stop > start);
	}
}

class Describe implements Command {
	private String size = "N";
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 3)
			throw new BadCommandException();
			
		if (args.length > 3)
			if (!Arrays.asList(Util.getSizes()).contains(args[3]))
				throw new BadCommandException();
			else
				size = args[3];
		String entry = "describe " + args[1] + " " 
						+ size + " " + args[2] + "\n";
		Logger.getInstance().writeToFile(entry);
	}
}

class Summary implements Command {
	private class TaskData {
		String size = "Not Set";
		String description = "Not Set";
		int time = 0;

		String size() {
			return size;
		}
	}

	private String[] startLine = null;
	private Map<String, TaskData> taskMap;
	private Map<String, Consumer<String[]>> parseMap;

	public void execute(String[] args) throws BadCommandException {
		if (parseMap == null)
			generateParseMap();
		parseLog();
		if (args.length > 1) {
			if (Arrays.asList(Util.getSizes()).contains(args[1])) {
				System.out.println("Summary for size: " + args[1]);
				printOutput(args[1], null);
			}
			else {
				printOutput(null, args[1]);
			}
		}
		else {
			printOutput(null, null);
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
		Logger.getInstance().getFileStream().map(Util::getWords)
			.forEach(line -> {
				if (parseMap.get(line[0]) == null)
					return;
				parseMap.get(line[0]).accept(line);
			});
	}

	private void printOutput(String size, String name) {
		taskMap.forEach((task, data) -> {
			if (printFilter(size, name, task, data)) {
				printOut(task, data.size, data.description, data.time);
			}
		});
		if (name == null)
			printAvgSpentTime(size);
	}

	private boolean printFilter(String size, String name, 
								String task, TaskData data) {
		return (name == null && size == null ||
				name == null && data.size.equals(size) ||
				size == null && task.equals(name)
				);
	}
	
	private void printAvgSpentTime(String size) {
		Map<String, List<TaskData>> sortedBySizeMap = taskMap.values()
					.stream().collect(Collectors.groupingBy(TaskData::size));

		sortedBySizeMap.forEach((dataSize, dataList) -> {
			if (dataList.size() > 1 
				&& Arrays.asList(Util.getSizes()).contains(dataSize) 
							&& (size == null || dataSize.equals(size))) {
				List<Integer> timeList = dataList.stream()
							.map(data -> data.time).toList();

				int[] min = Util.convertSecondsToTime(timeList.stream()
							.min(Integer::compare).get());
							
				int[] max = Util.convertSecondsToTime(timeList.stream()
							.max(Integer::compare).get());

				int[] avg = Util.convertSecondsToTime((int)timeList.stream()
							.mapToDouble(i -> i).average().orElse(0));

				System.out.println("Time Statistics for Size " + dataSize);
				System.out.println("Min: " + Util.formatTime(min) + "\tMax: " 
											+ Util.formatTime(max) + "\tAvg: " 
											+ Util.formatTime(avg) + "\n");
			}
		});
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
		if (startLine == null || startLine.length < 3 || endLine.length < 3) {
			System.err.println("Malformed Stop");
			return;
		}

		String startTask = startLine[1], endTask = endLine[1];
		if (!startTask.equals(endTask) || !taskMap.containsKey(endTask)) {
			System.err.println("Malformed Stop");
			return;
		}
		
		TaskData data = taskMap.get(startTask);
		try {
			LocalDateTime startTime = LocalDateTime.parse(startLine[2]);
			LocalDateTime endTime = LocalDateTime.parse(endLine[2]);
			int timeDelta = (int)startTime.until(endTime, ChronoUnit.SECONDS);
			if (timeDelta < 0 || LocalDateTime.now()
									.until(startTime, ChronoUnit.SECONDS) > 0
							|| LocalDateTime.now()
									.until(endTime, ChronoUnit.SECONDS) > 0) {
				System.err.println("Malformed Start/Stop");
				return;
			}
			data.time = timeDelta;
			taskMap.put(startTask, data);
		}
		catch (DateTimeParseException e) {
			System.err.println("Malformed Date and Time");
			return;
		}
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
		if (Arrays.asList(Util.getSizes()).contains(size)) {
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

	private void printOut(String task, String size, 
							String description, int seconds) {
		int[] time = Util.convertSecondsToTime(seconds);
 		System.out.println("Summary for size\t:" + task);
		System.out.println("Size of task\t\t:" + size);
		System.out.println("Description\t\t:" + description);
		System.out.println("Time spent on task\t:" 
							+ Util.formatTime(time) + "\n");
	}
}

class Size implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 3 
				|| !Arrays.asList(Util.getSizes()).contains(args[2]))
			throw new BadCommandException();
		else {
			String entry = "size " + args[1] + " " + args[2] + "\n";
			Logger.getInstance().writeToFile(entry);
		}
	}
}

class Rename implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 3 || !canRename() 
				|| Arrays.asList(Util.getSizes()).contains(args[1]))
			throw new BadCommandException();
		String entry = "rename " + args[1] + " " + args[2] + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canRename() {
		return Util.startBeforeStop();
	}
}

class Delete implements Command {
	public void execute(String[] args) throws BadCommandException {
		if (args.length < 2 || !canDelete())
			throw new BadCommandException();
		String entry = "delete " + args[1] + "\n";
		Logger.getInstance().writeToFile(entry);
	}

	private boolean canDelete() {
		return Util.startBeforeStop();
	}
}

class Util {
	public static String[] getSizes() {
		String[] sizes = {"S", "M", "L", "XL"};
		return sizes;
	}
	public static String[] getWords(String string) {
		return string.split("\\s+");
	}

	public static int[] convertSecondsToTime(int seconds) {
		int hours = seconds / 3600;
		int minutes = seconds % 3600 / 60;
		seconds = seconds % 60;
		return new int[] { hours, minutes, seconds };
	}

	public static String formatTime(int[] time) {
		return time[0] + ":" + time[1] + ":" + time[2];
	}

	public static String getCommand(String[] line) {
		return line[0];
	}

	public static boolean startBeforeStop() {
		int start = -1, stop = 0, i = 0;
		Scanner scanner = Logger.getInstance().getFileReader();
		while (scanner.hasNextLine()) {
			String[] line = Util.getWords(scanner.nextLine());
			if (line[0].equals("start"))
				start = i;
			if (line[0].equals("stop"))
				stop = i;
			i++;
		}
		scanner.close();
		return (stop > start);
	}
}