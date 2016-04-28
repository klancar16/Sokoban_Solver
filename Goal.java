package sokoban;

public class Goal {
	public Coordinate coor;
	public Coordinate prevCoor = null;
	public boolean filled;
	
	public Goal(byte x, byte y) {
		coor = new Coordinate(x, y);
		this.filled = false;
	}
}
