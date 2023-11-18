package com.example.gomokubygrpc;

import grpc.GameStatus;
import grpc.PlayerRole;

public class Game {

    private static final int FIELD_SIZE = 15;

    GameStatus gameStatus;

    private Cell[][] field = new Cell[FIELD_SIZE][FIELD_SIZE];

    public Game() {
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                field[i][j] = Cell.Empty;
            }
        }
    }

    private enum Cell {
        Empty, Cross, Zero
    }

    boolean turn(int rowIndex, int columnIndex, PlayerRole playerRole) {
        if (field[rowIndex][columnIndex] != Cell.Empty) {
            //fault
            return false;
        } else {
            field[rowIndex][columnIndex] = mapPlayerRoleToCell(playerRole);
            updateGameStatus(rowIndex, columnIndex, mapPlayerRoleToCell(playerRole));
        }
        return true;
    }

    private Cell mapPlayerRoleToCell(PlayerRole playerRole) {
        return playerRole == PlayerRole.Cross ? Cell.Cross : Cell.Zero;
    }

    private boolean checkRow(int rowIndex, Cell playerTurnCell) {
        int counter = 0;
        for (int column = 0; column < FIELD_SIZE; column++) {
            if (field[rowIndex][column] == playerTurnCell) {
                counter++;
                if (counter == 5) {
                    return true;
                }
            } else {
                counter = 0;
            }
        }
        return false;
    }

    private boolean checkColumn(int columnIndex, Cell playerTurnCell) {
        int counter = 0;
        for (int row = 0; row < FIELD_SIZE; row++) {
            if (field[row][columnIndex] == playerTurnCell) {
                counter++;
                if (counter == 5) {
                    return true;
                }
            } else {
                counter = 0;
            }
        }
        return false;
    }

    private boolean checkDiagonals(int rowIndex, int columnIndex, Cell playerTurnCell) {
        return checkDiagonalFromTopToLeft(rowIndex, columnIndex, playerTurnCell) || checkDiagonalFromTopToRight(rowIndex, columnIndex, playerTurnCell);
    }

    private boolean checkDiagonalFromTopToRight(int rowIndex, int columnIndex, Cell playerTurnCell) {
        int x = rowIndex;
        int y = columnIndex;
        while (x != 0 && y != 0) {
            x--;
            y--;
        }
        int counter = 0;
        while (x < FIELD_SIZE && y < FIELD_SIZE) {
            if (field[x][y] == playerTurnCell) {
                counter++;
                if (counter == 5) {
                    return true;
                }
            } else {
                counter = 0;
            }
            x++;
            y++;
        }
        return false;
    }

    private boolean checkDiagonalFromTopToLeft(int rowIndex, int columnIndex, Cell playerTurnCell) {
        int x = rowIndex;
        int y = columnIndex;
        while (x < FIELD_SIZE - 1 && y > 0) {
            x++;
            y--;
        }
        int counter = 0;
        while (x >= 0 && y < FIELD_SIZE) {
            if (field[x][y] == playerTurnCell) {
                counter++;
                if (counter == 5) {
                    return true;
                }
            } else {
                counter = 0;
            }
            x--;
            y++;
        }
        return false;
    }

    private boolean isDraw() {
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                if (field[i][j] == Cell.Empty) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateGameStatus(int rowIndex, int columnIndex, Cell playerTurnCell) {

        if (checkRow(rowIndex, playerTurnCell) || checkColumn(columnIndex, playerTurnCell) || checkDiagonals(rowIndex, columnIndex, playerTurnCell)) {
            gameStatus = playerTurnCell == Cell.Cross ? GameStatus.WinCross : GameStatus.WinZero;
        } else if (isDraw()) {
            gameStatus = GameStatus.Draw;
        } else {
            gameStatus = GameStatus.GameStarted;
        }
    }

}

