package grisu.control;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.python.google.common.collect.Sets;

public class JobnameHelpers {

	public static String calculateTimestampedJobname(String baseJobname) {
		final SimpleDateFormat format = new SimpleDateFormat(
				"yyyy.MM.dd_HH.mm.SSS");
		return calculateTimestampedJobname(baseJobname, format);
	}

	public static String calculateTimestampedJobname(String baseJobname,
			SimpleDateFormat format) {
		String newJobname = baseJobname + "_" + format.format(new Date());
		newJobname = newJobname.replace(":", "_");
		newJobname = newJobname.replace("\\", "_");
		newJobname = newJobname.replace("/", "_");
		newJobname = newJobname.replaceAll("\\s", "_");
		return newJobname;
	}

	public static String calculateUUIDJobname(String basejobname) {
		return basejobname + "_" + UUID.randomUUID().toString();
	}

	private static String convertGlobToRegEx(String line) {

		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		// Remove beginning and ending * globs because they're useless
		if (line.startsWith("*")) {
			line = line.substring(1);
			strLen--;
		}
		if (line.endsWith("*")) {
			line = line.substring(0, strLen - 1);
			strLen--;
		}
		boolean escaping = false;
		int inCurlies = 0;
		for (char currentChar : line.toCharArray()) {
			switch (currentChar) {
			case '*':
				if (escaping) {
					sb.append("\\*");
				} else {
					sb.append(".*");
				}
				escaping = false;
				break;
			case '?':
				if (escaping) {
					sb.append("\\?");
				} else {
					sb.append('.');
				}
				escaping = false;
				break;
			case '.':
			case '(':
			case ')':
			case '+':
			case '|':
			case '^':
			case '$':
			case '@':
			case '%':
				sb.append('\\');
				sb.append(currentChar);
				escaping = false;
				break;
			case '\\':
				if (escaping) {
					sb.append("\\\\");
					escaping = false;
				} else {
					escaping = true;
				}
				break;
			case '{':
				if (escaping) {
					sb.append("\\{");
				} else {
					sb.append('(');
					inCurlies++;
				}
				escaping = false;
				break;
			case '}':
				if ((inCurlies > 0) && !escaping) {
					sb.append(')');
					inCurlies--;
				} else if (escaping) {
					sb.append("\\}");
				} else {
					sb.append("}");
				}
				escaping = false;
				break;
			case ',':
				if ((inCurlies > 0) && !escaping) {
					sb.append('|');
				} else if (escaping) {
					sb.append("\\,");
				} else {
					sb.append(",");
				}
				break;
			default:
				escaping = false;
				sb.append(currentChar);
			}
		}
		return sb.toString();
	}

	public static Set<String> filterJobnamesUsingGlob(Collection<String> jobnames, String glob) {

		Pattern p = Pattern.compile(convertGlobToRegEx(glob));

		return filterJobnamesUsingRegex(jobnames, p);


	}

	public static Set<String> filterJobnamesUsingRegex(Collection<String> jobnames, Pattern pattern) {

		Set<String> result = Sets.newTreeSet();

		for (String name : jobnames) {
			Matcher matcher = pattern.matcher(name);
			boolean found = matcher.find();
			if (found) {
				result.add(name);
			}
		}

		return result;
	}

	public static void main(String[] args) {

		String[] names = new String[] { "gricli", "gricli2", "gricli3",
				"grisu", "grisu2" };

		String glob = "grisu2";
		Set<String> result = JobnameHelpers.filterJobnamesUsingGlob(
				Arrays.asList(names), glob);

		System.out.println(StringUtils.join(result, "\n"));

	}

}
