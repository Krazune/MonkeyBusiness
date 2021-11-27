package com.krazune.monkeybusiness;

public enum BusinessType
{
	GIGANTIC_PILE(1465),
	SMALL_PILE(1757),
	MEDIUM_PEBBLES(5690),
	SMALL_PEBBLES(11451),
	FLOOR_MARKS(1124);

	private final int value;

	BusinessType(int value)
	{
		this.value = value;
	}

	public int getValue()
	{
		return value;
	}
}
