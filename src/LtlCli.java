import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import jcurses.system.CharColor;
import jcurses.system.InputChar;
import jcurses.system.Toolkit;

public class LtlCli {

	private static final String MATCH_HEADING_IDENTIFIER = "’18.";
	private static final String SPLITTER = " XX ";
	private static final String SCORES_SPLITTER = " <vs.> ";
	private static final String FIRST_HALF = "1.  f é l i d õ";
	private static final int FIRST_HALF_INDEX = 1;
	private static final String SECOND_HALF = "2.  f é l i d õ";
	private static final int SECOND_HALF_INDEX = 8;
	private static final String EXTRA_MATCHES = "T a r t a l é k   m e c c s e k";
	private static final int EXTRA_MATCHES_INDEX = 15;
	private static final String DEFAULT_SCORE = "3";
	private static boolean autofillExtra = false;
	private static boolean briefMatchInfo = false;
	private static Path logger;

	public static void main(String[] args) {
		try {
			logger = Files.createFile(Paths.get("d:\\ltl.log"));
		} catch (IOException e) {
			System.out.println("error while creating log file: " + e.getMessage());
		}
		if (args.length < 1) {
			log("required argument: file name with path");
			log("optional switches: autoextra, brief");
			return;
		}
		autofillExtra = System.getProperty("autoextra") != null;
		briefMatchInfo = System.getProperty("brief") != null;

		List<XWPFParagraph> paragraphs = readParagraphsFromDocx(args[0]);
		List<String> matches = parseMatchesFromRawParagraphs(paragraphs);
		List<String> results = collectResultsFromConsole(matches);
		StringBuilder finalResult = composeFinalResult(results);
		copyFinalResultToClipboard(finalResult);
	}

	private static void log(String message) {
		try {
			Files.write(logger, message.getBytes(), StandardOpenOption.APPEND);
			System.out.println(message);
		} catch (IOException e) {
			System.out.println("error while logging to file: " + e.getMessage());
		}
	}

	private static void log(StringBuilder message) {
		log(message.toString());
	}
	
	private static void copyFinalResultToClipboard(StringBuilder finalResult) {
		log(finalResult);
		Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection selection = new StringSelection(finalResult.toString());
		c.setContents(selection, selection);
	}

	private static List<XWPFParagraph> readParagraphsFromDocx(String path) {
		List<XWPFParagraph> paragraphs = new ArrayList<>();
		try {
			FileInputStream fis = new FileInputStream(path);
			XWPFDocument doc = new XWPFDocument(fis);
			paragraphs = doc.getParagraphs();
			doc.close();
			return paragraphs;
		} catch (FileNotFoundException fnfe) {
			log("file not found: " + path + ", exception: " + fnfe.getLocalizedMessage());
		} catch (IOException ioe) {
			log("I/O issue with " + path + ", exception: " + ioe.getLocalizedMessage());
		}
		return paragraphs;
	}

	private static List<String> collectResultsFromConsole(List<String> matches) {
		List<String> results = new ArrayList<>();
		InputChar ch = null;
		String home = DEFAULT_SCORE;
		String away = DEFAULT_SCORE;
		CharColor color = new CharColor(CharColor.BLACK, CharColor.WHITE);
		Toolkit.init();
		Toolkit.clearScreen(color);

		int count = 0;
		for (String match : matches) {
			count++;
			if (count < EXTRA_MATCHES_INDEX || (count >= EXTRA_MATCHES_INDEX && !autofillExtra)) {
				System.out
						.println(briefMatchInfo ? match.substring(match.indexOf(SPLITTER) + SPLITTER.length()) : match);
				ch = Toolkit.readCharacter();
				home = Character.toString(ch.getCharacter());
				System.out.print(ch + " X ");
				ch = Toolkit.readCharacter();
				away = ch.toString();
				log(away);
			} else {
				home = DEFAULT_SCORE;
				away = DEFAULT_SCORE;
			}
			results.add(match + SPLITTER + home + SCORES_SPLITTER + away);
		}

		return results;
	}

	private static StringBuilder composeFinalResult(List<String> collectedResults) {
		StringBuilder finalResult = new StringBuilder();
		String[] split = null;
		String heading = "";
		String homeTeam = "";
		String scores = "";
		String awayTeam = "";
		int counter = 1;
		for (String result : collectedResults) {
			split = result.split(SPLITTER);
			heading = split[0];
			homeTeam = split[1].substring(0, split[1].indexOf(SCORES_SPLITTER));
			awayTeam = split[1].substring(split[1].indexOf(SCORES_SPLITTER) + SCORES_SPLITTER.length());
			scores = split[2].replace(SCORES_SPLITTER, " X ");
			switch (counter) {
			case FIRST_HALF_INDEX:
				finalResult.append("\n").append(FIRST_HALF).append("\n\n");
				break;
			case SECOND_HALF_INDEX:
				finalResult.append("\n").append(SECOND_HALF).append("\n\n");
				break;
			case EXTRA_MATCHES_INDEX:
				finalResult.append("\n").append(EXTRA_MATCHES).append("\n\n");
				break;
			default:
				break;
			}
			finalResult.append(heading).append("\n").append(homeTeam).append("\n").append(scores).append("\n")
					.append(awayTeam).append("\n\n");
			counter++;
		}
		return finalResult;
	}

	private static List<String> parseMatchesFromRawParagraphs(List<XWPFParagraph> paragraphs) {
		List<String> result = new ArrayList<>();
		int i = 0;
		String row = "";
		HashMap<Integer, Integer> map = new HashMap<>();
		boolean extra = false;
		int index = 0;
		try {
			for (XWPFParagraph p : paragraphs) {
				row = p.getText();
				i++;
				if (row.contains(EXTRA_MATCHES))
					extra = true;
				if (row.contains(MATCH_HEADING_IDENTIFIER)) {
					index = Integer.parseInt(row.substring(0, row.indexOf(".")).trim());
					map.put(extra ? index + EXTRA_MATCHES_INDEX - 1 : index, i - 1);
				}
			}
			String match = "";
			for (Integer key : map.keySet()) {
				match = paragraphs.get(map.get(key)).getText() + SPLITTER + paragraphs.get(map.get(key) + 1).getText()
						+ SCORES_SPLITTER + paragraphs.get(map.get(key) + 3).getText();
				result.add(match);
			}
		} catch (Exception e) {
			log("exception at processing paragraphs: " + e.getLocalizedMessage());
		}
		return result;
	}
}
