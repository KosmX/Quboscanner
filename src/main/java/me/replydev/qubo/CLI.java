package me.replydev.qubo;

import dev.kosmx.scannerMod.IpListList;
import me.replydev.utils.FileUtils;
import me.replydev.utils.KeyboardThread;
import me.replydev.utils.Log;
import me.replydev.versionChecker.VersionChecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CLI {

	private static QuboInstance quboInstance;

	public static QuboInstance getQuboInstance() 
	{
		return quboInstance;
	}

	static void init(String[] a) 
	{
		printLogo();
		if(!isUTF8Mode()){
			System.out.println("The scanner isn't running in UTF-8 mode!");
			System.out.println("Put \"-Dfile.encoding=UTF-8\" in JVM args in order to run the program correctly!");
			System.exit(-1);
		}
		VersionChecker.checkNewVersion();
		FileUtils.createFolder("outputs");
		ExecutorService inputService = Executors.newSingleThreadExecutor();
		inputService.execute(new KeyboardThread());
		//if (Arrays.equals(new String[] { "-txt" }, a))
		//	txtRun();
		//else
		standardRun(a);
		Log.logln("Scan terminated - " + Info.serverFound + " (" + Info.serverNotFilteredFound + " in total)");
		System.exit(0);
	}

	private static void printLogo()
	{
		System.out.println("""
				  ____        _           _____                                \s
				 / __ \\      | |         / ____|                               \s
				| |  | |_   _| |__   ___| (___   ___ __ _ _ __  _ __   ___ _ __\s
				| |  | | | | | '_ \\ / _ \\\\___ \\ / __/ _` | '_ \\| '_ \\ / _ \\ '__|
				| |__| | |_| | |_) | (_) |___) | (_| (_| | | | | | | |  __/ |  \s
				 \\___\\_\\\\__,_|_.__/ \\___/_____/ \\___\\__,_|_| |_|_| |_|\\___|_|  \s
				                                                              \s""".indent(1));
		System.out.println(
				"By @replydev on Telegram\nVersion " + Info.version + " " + Info.otherVersionInfo);
	}

	private static void standardRun(String[] a)
	{
		InputData i;
		try 
		{
			i = new InputData(a);
		} 
		catch (Exception e) 
		{
			System.err.println(e.getMessage());
			return;
		}
		Info.debugMode = i.isDebugMode();
		quboInstance = new QuboInstance(i);
		try{
			quboInstance.run();
		}catch (NumberFormatException e){
			quboInstance.inputData.help();
		}
	}

	public static IpListList txtGet()
	{
		try (BufferedReader reader = Files.newBufferedReader(Path.of("ranges.txt")))
		{
			return IpListList.Companion.of(reader);
		} 
		catch (IOException e) 
		{
			System.err.println("File \"ranges.txt\" not found, create a new one and restart the scanner");
			System.exit(-1);
			throw new AssertionError("");
		}
	}

	private static boolean isUTF8Mode()
	{
		List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		return arguments.contains("-Dfile.encoding=UTF-8");
	}

}