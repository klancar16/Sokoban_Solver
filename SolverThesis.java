package sokoban;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.lang3.time.StopWatch;

public class SolverThesis {
	static Box[] boxes; //
	static Goal[] goals; //
	static char[][] startingMap;//
	static char[][] groundMap;//
	static int[][] obviousDeadlocks;//
	static char[][] btBuilding; //
	static boolean solution;//
	static Coordinate startPos;//
	static short minStep;//
	static List<Character> moves;//
	//static long nana = 0;
	static List<String> solutions;//
	static Coordinate illegalBoxPosMin;//
	static Coordinate illegalBoxPosMax;//
	//static int neki = 0;
	static ArrayList<ArrayList<ArrayList<String>>> beenThereTable;//
	static ArrayList<ArrayList<ArrayList<Short>>> beenThereStepTable;//
	static StopWatch st;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// j - box
		// g - goal
		// m - sokoban
		
		List<String> linesToWrite = new ArrayList<String>();
		
		for(int iter = 1; iter <= 155; iter++) {
			//StopWatch st = new StopWatch();
			System.out.print(iter + "/" + 155 + "  ");
			st = new StopWatch();
			st.start();
			String fromFile = "";
			try {
				fromFile = new String(Files.readAllBytes(Paths.get("microban/maze"+iter+".txt")));
				//fromFile = new String(Files.readAllBytes(Paths.get("example_map.txt")));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(fromFile);
			if(fromFile.equals("")) {
				System.out.println("File was not read");
				System.exit(0);
			}
			minStep = 1000;
			solution = false;
			String[] lines = getLines(fromFile);
			String[] mapData = lines[0].split(" ");
			int xLen = Integer.parseInt(mapData[1]);
			int yLen = Integer.parseInt(mapData[0]);
			int noOfGoals = Integer.parseInt(mapData[2]);
			boxes = new Box[noOfGoals];
			goals = new Goal[noOfGoals];
			
			char[][] map = new char[xLen][yLen];
			groundMap = new char[xLen][yLen];
			btBuilding = new char[xLen][yLen];
			map = stringToMap(lines, map);
			startingMap = map;
			
			moves = new ArrayList<Character>();
			solutions = new ArrayList<String>();
			obviousDeadlocks = new int[xLen][yLen];
			obviousDeadlocks = findObviousDeadlocks(obviousDeadlocks);
			
			byte[] boxesPos = new byte[boxes.length*2];
			for(int i = 0; i < boxes.length; i++) {
				boxesPos[2*i] = boxes[i].coor.x;
				boxesPos[2*i +1] = boxes[i].coor.y;
				//System.out.println(i + ": " + boxesPos[2*i] + " " + boxesPos[2*i +1] );
			}
			/*for(int i = 0; i < goals.length; i++) {
				System.out.println(goals[i].coor.toString());
			}*/
			//System.out.println(startPos.toString());
			//printMap(groundMap);
			solve(boxesPos, new byte[]{startPos.x, startPos.y}, new byte[]{startPos.x, startPos.y}, (short) 0, false);
			
			//System.out.println(nana);
			/*System.out.println(solutions.size());
			for(int i  = 0; i < solutions.size(); i++) {
				System.out.println(solutions.get(i).length());
			}*/
			String finalSolution = "no solution";
			int solutionLenght = 0;
			if(solutions.size() > 0) {
				//System.out.println(solutions.get(solutions.size()-1));
				finalSolution = solutions.get(solutions.size()-1);
				solutionLenght = finalSolution.length();
			}
			st.stop();
		    long time = st.getNanoTime();
		    double seconds = (double) time / 1000000000.0;
		    System.out.println(seconds + " s");
		    linesToWrite.add(iter + ". " + solutionLenght + " | " + time);
		}
		Path file = Paths.get("results.txt");
		try {
			Files.write(file, linesToWrite, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void solve(byte[] boxesPos, byte[] lastPos, byte[] cur, short step, boolean boxMoved) {
		// TODO Auto-generated method stub
		//nana++;
		
		long time = st.getNanoTime();
	    double seconds = (double) time / 1000000000.0;
	    if(seconds > 600) {
	    	return;
	    }
		/* build string for beenthere */
		String bT = ""; // = cur[0] + "" + cur[1];
		for(int b = 0; b < boxesPos.length/2; b++) {
			//bT = bT + "" + boxesPos[2*b] + "" + boxesPos[2*b+ 1];
			btBuilding[boxesPos[2*b]][boxesPos[2*b+ 1]] = 'b';
		}
		for(int i = 0; i < btBuilding.length; i++) {
			for (int j = 0; j < btBuilding[0].length; j++) {
				if(btBuilding[i][j] == 'b') {
					bT = bT + "" + i + "" + j;
					btBuilding[i][j] = ' ';
				}
			}
		}
		
		
		//step
		int cbtIdx = checkBeenThere(bT, cur);
		if(cbtIdx != -1 && (beenThereStepTable.get(cur[0]).get(cur[1]).get(cbtIdx) <= step)) {
			return;
		}
		if(cbtIdx == -1) {
			beenThereTable.get(cur[0]).get(cur[1]).add(bT);
			beenThereStepTable.get(cur[0]).get(cur[1]).add((short) step);
		}
		else {
			beenThereStepTable.get(cur[0]).get(cur[1]).set(cbtIdx, (short) step);
		}
		
		
		if(step >= minStep) {
			return;
		}
		
		if(solved(cur, boxesPos)) {
			minStep = (short) (step+1);
			String string = writeOut();
			//System.out.println(step);
			solutions.add(string);
			return;
		}
		
		byte[] forPast = new byte[]{cur[0], cur[1]};
		List<String> priorityList = getPriorityList(cur, boxesPos);
		//List<String> priorityList = new ArrayList<String>();
		//priorityList.add("L"); priorityList.add("U"); priorityList.add("R"); priorityList.add("D");
		for(int movIdx = 0; movIdx < 4; movIdx++) {
			if(priorityList.get(movIdx).equals("R")) {
				if((!(lastPos[0] == cur[0] && lastPos[1] == cur[1]+1) || boxMoved) && !(groundMap[cur[0]][cur[1]+1] == 'X')) {
					//moves.add('R');
					boolean moveBox = false;
					byte prevBoxPos = 0;
					byte idx = 0;
					for(byte i = 0; i < boxesPos.length/2; i++) {
						if(boxesPos[2*i] == cur[0] && boxesPos[2*i + 1] == cur[1]+1) {
							prevBoxPos = boxesPos[2*i + 1];
							idx = i;
							boxesPos[2*i + 1] = (byte) (boxesPos[2*i + 1]+1);
							moveBox = true;
							break;
						}
					}
					/*if(!(boxesProblem(boxesPos))) {
						solve(boxesPos, cur, new byte[]{cur[0], (byte) (cur[1]+1)}, (short) (step+1), (short) (cost+1), moveBox);
					}*/
					if(moveBox) {
						moves.add('R');
					}
					else {
						moves.add('r');
					}
					if(!(boxesProblem(boxesPos))) {
						cur[1] = (byte) (cur[1]+1);
						solve(boxesPos, forPast, cur, (short) (step+1), moveBox);
						cur[1] = (byte) (cur[1]-1);
					}
					if(moveBox) {
						boxesPos[2*idx + 1] = prevBoxPos;
					}
					moves.remove(moves.size()-1);
				}
			}
			
			if(priorityList.get(movIdx).equals("U")) {
				if((!(lastPos[0] == cur[0]-1 && lastPos[1] == cur[1]) || boxMoved) && !(groundMap[cur[0]-1][cur[1]] == 'X')) {
					//moves.add('U');
					boolean moveBox = false;
					byte prevBoxPos = 0;
					byte idx = 0;
					for(byte i = 0; i < boxesPos.length/2; i++) {
						if(boxesPos[2*i] == cur[0]-1 && boxesPos[2*i + 1] == cur[1]) {
							prevBoxPos = boxesPos[2*i];
							idx = i;
							boxesPos[2*i] = (byte) (boxesPos[2*i]-1);
							moveBox = true;
							break;
						}
					}
					if(moveBox) {
						moves.add('U');
					}
					else {
						moves.add('u');
					}
					/*if(!(boxesProblem(boxesPos))) {
						solve(boxesPos, cur, new byte[]{(byte) (cur[0]-1), cur[1]}, (short) (step+1), (short) (cost+1), moveBox);
					}*/
					if(!(boxesProblem(boxesPos))) {
						cur[0] = (byte) (cur[0]-1);
						solve(boxesPos, forPast, cur, (short) (step+1), moveBox);
						cur[0] = (byte) (cur[0]+1);
					}
					if(moveBox) {
						boxesPos[2*idx] = prevBoxPos;
					}
					moves.remove(moves.size()-1);
				}
			}
			
			if(priorityList.get(movIdx).equals("D")) {
				if((!(lastPos[0] == cur[0]+1 && lastPos[1] == cur[1]) || boxMoved) && !(groundMap[cur[0]+1][cur[1]] == 'X')) {
					//moves.add('D');
					boolean moveBox = false;
					byte prevBoxPos = 0;
					byte idx = 0;
					for(byte i = 0; i < boxesPos.length/2; i++) {
						if(boxesPos[2*i] == cur[0]+1 && boxesPos[2*i + 1] == cur[1]) {
							prevBoxPos = boxesPos[2*i];
							idx = i;
							boxesPos[2*i] = (byte) (boxesPos[2*i]+1);
							moveBox = true;
							break;
						}
					}
					if(moveBox) {
						moves.add('D');
					}
					else {
						moves.add('d');
					}
					/*if(!(boxesProblem(boxesPos))) {
						solve(boxesPos, cur, new byte[]{(byte) (cur[0]+1), cur[1]}, (short) (step+1), (short) (cost+1), moveBox);
					}*/
					if(!(boxesProblem(boxesPos))) {
						cur[0] = (byte) (cur[0]+1);
						solve(boxesPos, forPast, cur, (short) (step+1), moveBox);
						cur[0] = (byte) (cur[0]-1);
					}
					if(moveBox) {
						boxesPos[2*idx] = prevBoxPos;
					}
					moves.remove(moves.size()-1);
				}
			}
			
			if(priorityList.get(movIdx).equals("L")) {
				if((!(lastPos[0] == cur[0] && lastPos[1] == cur[1]-1) || boxMoved) && !(groundMap[cur[0]][cur[1]-1] == 'X')) {
					//moves.add('L');
					boolean moveBox = false;
					byte prevBoxPos = 0;
					byte idx = 0;
					for(byte i = 0; i < boxesPos.length/2; i++) {
						if(boxesPos[2*i] == cur[0] && boxesPos[2*i + 1] == cur[1]-1) {
							prevBoxPos = boxesPos[2*i + 1];
							idx = i;
							boxesPos[2*i + 1] = (byte) (boxesPos[2*i + 1] - 1);
							moveBox = true;
							break;
						}
					}
					if(moveBox) {
						moves.add('L');
					}
					else {
						moves.add('l');
					}
					/*if(!(boxesProblem(boxesPos))) {
						solve(boxesPos, cur, new byte[]{cur[0], (byte) (cur[1]-1)}, (short) (step+1), (short) (cost+1), moveBox);
					}*/
					if(!(boxesProblem(boxesPos))) {
						cur[1] = (byte) (cur[1]-1);
						solve(boxesPos, forPast, cur, (short) (step+1), moveBox);
						cur[1] = (byte) (cur[1]+1);
					}
					if(moveBox) {
						boxesPos[2*idx + 1] = prevBoxPos;
					}
					moves.remove(moves.size()-1);
				}
			}
		}
		
		//beenThere.remove(beenThere.size()-1);
		return;
		
	}

	private static int checkBeenThere(String bT, byte[] cur) {
		// TODO Auto-generated method stub
		int index = -1;
		List<String> listBT = beenThereTable.get(cur[0]).get(cur[1]);
		for(int i = 0; i < listBT.size(); i++) {
			if(bT.equals(listBT.get(i))) {
				return i;
			}
		}
		return index;
	}

	private static int[][] findObviousDeadlocks(int[][] obviousDeadlocks) {
		// TODO Auto-generated method stub
		/*for(int i = 1; i < groundMap.length-1; i++) {
			boolean goalInThisLine = false;
			if(i == illegalBoxPosMin.x || i == illegalBoxPosMax.x) {
				for (int j = 0; j < goals.length; j++) {
					if(goals[j].coor.x == i) {
						goalInThisLine = true;
					}
				}
				if(!goalInThisLine) {
					for (int j = 0; j < groundMap[i].length-1; j++) {
						obviousDeadlocks[i][j] = 1;
					}
				}
			}
		}
		for(int i = 1; i < groundMap[1].length-1; i++) {
			boolean goalInThisLine = false;
			if(i == illegalBoxPosMin.y || i == illegalBoxPosMax.y) {
				for (int j = 0; j < goals.length; j++) {
					if(goals[j].coor.y == i) {
						goalInThisLine = true;
					}
				}
				if(!goalInThisLine) {
					for (int j = 0; j < groundMap.length-1; j++) {
						obviousDeadlocks[j][i] = 1;
					}
				}
			}
		}*/
		
		//printMap(groundMap);
		
		for(int i = 1; i < groundMap.length-1; i++) {
			//boolean deady = false;
			int idx = 0;
			int len = 0;
			for(int j = 0; j < groundMap[i].length; j++) {
				if(idx == 0) {
					if(groundMap[i][j] == 'X') {
						idx = j+1;
					}
				}
				else {
					if(groundMap[i][j] == 'X') {
						len = j - idx;
						if(len == 0) {
							idx = 0;
						}
						else {
							addToDeadlocks(true, i, idx, len);
							idx = 0;
							len = 0;
						}
						j = j -1;
					}
					else if(groundMap[i][j] == 'G') {
						idx = 0;
					}
					else {
						if((groundMap[i-1][j] != 'X') && (groundMap[i+1][j] != 'X')) idx = 0;
					}
				}
			}
		}
		
		for(int i = 1; i < groundMap[1].length-1; i++) {
			//boolean deady = false;
			int idx = 0;
			int len = 0;
			for(int j = 0; j < groundMap.length; j++) {
				if(idx == 0) {
					if(groundMap[j][i] == 'X') {
						idx = j+1;
					}
				}
				else {
					if(groundMap[j][i] == 'X') {
						len = j - idx;
						if(len == 0) {
							idx = 0;
						}
						else {
							addToDeadlocks(false, i, idx, len);
							idx = 0;
							len = 0;
						}
						j = j -1;
					}
					else if(groundMap[j][i] == 'G') {
						idx = 0;
					}
					else {
						if((groundMap[j][i-1] != 'X') && (groundMap[j][i+1] != 'X')) idx = 0;
					}
				}
			}
		}
		
		for(int i = 1; i < groundMap.length-1; i++) {
			for(int j = 1; j < groundMap[i].length-1; j++) {
				if((groundMap[i][j+1] == 'X' || groundMap[i][j-1] == 'X') && (groundMap[i-1][j] == 'X' || groundMap[i+1][j] == 'X') &&
						(groundMap[i][j] != 'G')) {
					obviousDeadlocks[i][j] = 1;
				}
			}
		}
		
		/*for(int i = 0; i < groundMap.length; i++) {
			for(int j = 0; j < groundMap[i].length; j++) {
				System.out.print(obviousDeadlocks[i][j]);
			}
			System.out.println();
		}*/
		return obviousDeadlocks;
	}


	private static void addToDeadlocks(boolean isX, int coor, int idx, int len) {
		// TODO Auto-generated method stub
		//System.out.println(isX + " " + " " + coor + " " + idx + " " + len);
		
		if(isX) {
			for(int i = idx; i < idx+len; i++) {
				obviousDeadlocks[coor][i] = 1;
			}
		}
		else {
			for(int i = idx; i < idx+len; i++) {
				obviousDeadlocks[i][coor] = 1;
			}
		}
	}

	private static List<String> getPriorityList(byte[] cur, byte[] boxesPos) {
		// TODO Auto-generated method stub
		List<String> list = new ArrayList<String>();
		list.add("R"); list.add("U"); list.add("D"); list.add("L");
		byte[] distances = new byte[4];
		for(int i = 0; i < 4; i++) {
			byte sumDis = 0;
			/* R */
			byte x = cur[0];
			byte y = (byte) (cur[1]+1);
			if(i == 1) /* U */ {
				x = (byte) (cur[0]-1);
				y = cur[1];
			}
			else if(i == 2) /* D */ {
				x = (byte) (cur[0]+1);
				y = cur[1];
			}
			else if(i == 3) /* L */ {
				x = cur[0];
				y = (byte) (cur[1]-1);
			}
			for(int j = 0; j < boxesPos.length/2; j++) {
				byte dis = (byte) (Math.abs(x - boxesPos[2*j]) + Math.abs(y - boxesPos[2*j + 1]));
				if(dis == 0) {
					if(onGoal(x, y)){
						dis = 5;
					}
					if(awayFromGoal(cur[0], cur[1], x, y, boxesPos)) {
						dis = (byte) (dis + 5);
					}
				}
				sumDis = (byte) (sumDis + dis);
			}
			distances[i] = (byte) sumDis;
		}
		
		for(int i = 0; i < 4; i++) {
			for(int j = i; j < 4; j++) {
				if(distances[i] > distances[j]) {
					String temp = list.get(i);
					list.set(i, list.get(j));
					list.set(j, temp);
					
					byte tmp = distances[i];
					distances[i] = distances[j];
					distances[j] = tmp;
				}
			}
		}
		/*for(int i = 0; i < 4; i++) {
			System.out.println(list.get(i) + " " + distances[i]);
		}*/
		return list;
	}

	private static boolean awayFromGoal(byte curX, byte curY, byte x, byte y, byte[] boxesPos) {
		// TODO Auto-generated method stub
		int currentBoxX = x;
		int currentBoxY = y;
		
		boolean isX = false;
		if(curY == y) {
			isX = true;
		}
		
		int futBoxX = x;
		int futBoxY = y;
		if(isX) {
			if(curX < futBoxX) futBoxX = futBoxX + 1;
			else futBoxX = futBoxX - 1;
		}
		else {
			if(curY < futBoxY) futBoxY = futBoxY + 1;
			else futBoxY = futBoxY - 1;
		}
		
		int curDis = 0;
		int futDis = 0;
		
		for(int i = 0; i < goals.length; i++) {
			if(futBoxX == goals[i].coor.x && futBoxY == goals[i].coor.y) {
				return false;
			}
			curDis = curDis + Math.abs(currentBoxX - goals[i].coor.x) + Math.abs(currentBoxY - goals[i].coor.y);
			futDis = futDis + Math.abs(futBoxX - goals[i].coor.x) + Math.abs(futBoxY - goals[i].coor.y);
		}
		if(futDis >= curDis) {
			return true;
		}
		
		return false;
	}

	private static boolean onGoal(int x, int y) {
		// TODO Auto-generated method stub

		if(groundMap[x][y] == 'G') {
			return true;
		}
		return false;
	}

	private static boolean boxesProblem(byte[] boxesPos) {
		// TODO Auto-generated method stub
		for(int i = 0; i < boxesPos.length/2; i++) {
			for(int j = i+1; j < boxesPos.length/2; j++) {
				if(boxesPos[2*i] == boxesPos[2*j] && boxesPos[2*i + 1] == boxesPos[2*j + 1]) return true;
			}
		}
		
		for(int i = 0; i < boxesPos.length/2; i++) {
			if(groundMap[boxesPos[2*i]][boxesPos[2*i + 1]] == 'X') {
				return true;
			}
			if(obviousDeadlocks[boxesPos[2*i]][boxesPos[2*i + 1]] == 1) {
				return true;
			}
		}
		return false;
	}


	private static boolean solved(byte[] cur, byte[] boxesPos) {
		// TODO Auto-generated method stub
		int count = 0;
		for(int i = 0; i < boxesPos.length/2; i++) {
			for(int j = 0; j < goals.length; j++) {
				if(boxesPos[2*i] == goals[j].coor.x && boxesPos[2*i + 1] == goals[j].coor.y) {
					count++;
					break;
				}
			}
		}
		if(count == goals.length) {
			return true;
		}
		return false;
	}

	private static String writeOut() {
		// TODO Auto-generated method stub
		String result = "";
		for(int i = 0; i < moves.size(); i++) {
			result = result + moves.get(i);
			//System.out.print(moves.get(i));
		}
		//System.out.println();
		return result;
		
	}


	private static void printMap(char[][] map) {
		// TODO Auto-generated method stub
		for(int i = 0; i < map.length; i++) {
			//System.out.print(i);
			for(int j = 0; j < map[i].length; j++) {
				if(map[i][j] == '.' && obviousDeadlocks[i][j] == 1) {
					System.out.print("1");
					continue;
				}
				System.out.print(map[i][j]);
			}
			System.out.println();
		}
		
	}

	private static String[] getLines(String fromFile) {
		// TODO Auto-generated method stub
		List<String> lines = new ArrayList<String>();
		Scanner scanner = new Scanner(fromFile);
		while (scanner.hasNextLine()) {
		  String line = scanner.nextLine();
		  lines.add(line);
		}
		scanner.close();
		return lines.toArray(new String[lines.size()]);
	}

	private static char[][] stringToMap(String[] lines, char[][] map) {
		// TODO Auto-generated method stub
		int noOfBoxes = 0;
		int noOfGoals = 0;
		byte xMin = (byte) 127;
		byte xMax = 0;
		byte yMin = (byte) 127;
		byte yMax = 0;
		beenThereTable = new ArrayList<>();
		beenThereStepTable = new ArrayList<>();
		int maxSize = 0;
		for(byte i = 1; i < lines.length; i++) {
			//System.out.println(lines[i].length());
			beenThereTable.add(new ArrayList<>());
			beenThereStepTable.add(new ArrayList<>());
			if(lines[i].length() > maxSize) {
				maxSize = lines[i].length();
			}
			for(byte j = 0; j < maxSize; j++) {
				map[i-1][j] = ' ';
				groundMap[i-1][j] = ' ';
				if(j >= lines[i].length()) {
					continue;
				}
				beenThereTable.get(i-1).add(new ArrayList<>());
				beenThereStepTable.get(i-1).add(new ArrayList<>());
				char curChar = lines[i].charAt(j);
				map[i-1][j] = curChar;
				groundMap[i-1][j] = curChar;
				if(curChar != 'X' && curChar != ' ') {
					if(i-1 < xMin) {
						xMin = (byte) (i-1);
					}
					if(i-1 > xMax) {
						xMax = (byte) (i-1);
					}
					if(j < yMin) {
						yMin = (byte) j;
					}
					if(j > yMax) {
						yMax = (byte) j;
					}
						
				}
				if(curChar == 'J') {
					boxes[noOfBoxes] = new Box((byte) (i-1), j, false);
					noOfBoxes++;
					groundMap[i-1][j] = '.';
				}
				else if(curChar == 'G') {
					goals[noOfGoals] = new Goal((byte) (i-1), j);
					noOfGoals++;
				}
				else if(curChar == 'M') {
					map[i-1][j] = 'M';
					startPos = new Coordinate((byte) (i-1), j);
					groundMap[i-1][j] = '.';
				}
				else if(curChar == '*') {
					map[i-1][j] = '*';
					goals[noOfGoals] = new Goal((byte) (i-1), j);
					noOfGoals++;
					boxes[noOfBoxes] = new Box((byte) (i-1), j, false);
					noOfBoxes++;
					groundMap[i-1][j] = 'G';
				}
				else if(curChar == '+') {
					map[i-1][j] = '+';
					startPos = new Coordinate((byte) (i-1), j);
					goals[noOfGoals] = new Goal((byte) (i-1), j);
					noOfGoals++;
					groundMap[i-1][j] = 'G';
				}
				
			}
		}
		illegalBoxPosMax = new Coordinate(xMax, yMax);
		illegalBoxPosMin = new Coordinate(xMin, yMin);
		return map;
	}
	
	private static void printMapWithM(int[] cur) {
		// TODO Auto-generated method stub
		for(int i = 0; i < groundMap.length; i++) {
			for(int j = 0; j < groundMap[i].length; j++) {
				if(i == cur[0] && j == cur[1]) {
					System.out.print('M');
				} else {
					System.out.print(groundMap[i][j]);
				}
				
			}
			System.out.println();
		}
		
	}
	
	private static void printMapWithMandB(byte[] cur, byte[] boxesPos) {
		// TODO Auto-generated method stub
		int[][] boxes = new int[groundMap.length][groundMap[0].length];
		for(int i = 0; i < boxesPos.length/2; i++) {
			boxes[boxesPos[2*i]][boxesPos[2*i + 1]] = 1;
		}
		for(int i = 0; i < groundMap.length; i++) {
			for(int j = 0; j < groundMap[i].length; j++) {
				if(i == cur[0] && j == cur[1]) {
					System.out.print('M');
				} else if(boxes[i][j] == 1) { 
					System.out.print('B');
				} else {
					System.out.print(groundMap[i][j]);
				}
				
			}
			System.out.println();
		}

		
	}



	
}
