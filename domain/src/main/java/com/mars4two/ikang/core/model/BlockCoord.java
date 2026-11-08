package com.mars4two.ikang.core.model;

/**
 * Pure domain 3D block coordinate.
 * Field names must remain {@code x}, {@code y}, and {@code z} to preserve backend JSON serialization parity.
 */
public record BlockCoord(int x, int y, int z) {
}
