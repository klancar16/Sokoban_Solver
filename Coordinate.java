package sokoban;

public class Coordinate {
	public byte x;
	public byte y;
	
	public Coordinate(byte x, byte y) {
		this.x = x;
		this.y = y;
	}
	
	public boolean sameCoor(Coordinate c2) {
		if(c2.x == this.x && c2.y == this.y) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		return (this.x + " " + this.y);
	}
}
