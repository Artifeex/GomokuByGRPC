package com.example.gomokubygrpc;

import com.google.protobuf.Empty;
import grpc.*;
import grpc.EnemyTurnMessage;
import grpc.GameServiceGrpc;
import grpc.GameStatus;
import grpc.PlayerRole;
import grpc.StartGameMessage;
import grpc.TurnMessageRequest;
import grpc.TurnMessageResponse;
import io.grpc.ManagedChannelBuilder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import io.grpc.Channel;

public class PlayerController {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    @FXML
    GridPane gridPane;

    @FXML
    Label labelMyRole;

    @FXML
    Label labelActiveRole;

    @FXML
    Label labelGameResult;

    private PlayerRole myRole;
    private PlayerRole activeRole;
    private GameStatus gameStatus;

    private GameServiceGrpc.GameServiceBlockingStub client;


    @FXML
    void onButtonClick(ActionEvent event) {
        if (activeRole == myRole && gameStatus == GameStatus.GameStarted) {
            Button button = (Button) event.getSource();
            int rowIndex = GridPane.getRowIndex(button) == null ? 0 : GridPane.getRowIndex(button);
            int columnIndex = GridPane.getColumnIndex(button) == null ? 0 : GridPane.getColumnIndex(button);

            grpc.TurnMessageRequest request = TurnMessageRequest.newBuilder()
                    .setRowIndex(rowIndex)
                    .setColumnIndex(columnIndex)
                    .setMadeMove(myRole)
                    .build();
            TurnMessageResponse response = client.makeTurn(request);
            if(response.getIsCorrectTurn()) {
                activeRole = response.getActiveRole();
                gameStatus = response.getGameStatus();
                updateLabels();
                updateGridByTurn(response.getRowIndex(), response.getColumnIndex(), response.getMadeMove());
            } else {
                //потенциально можно было бы сообщить, что именно не так произошло
            }
        }
    }

    private Button findButton(int rowIndex, int columnIndex) {
        Button result = null;
        for (Node node : gridPane.getChildren()) {
            int nodeRowIndex = GridPane.getRowIndex(node) == null ? 0 : GridPane.getRowIndex(node);
            int nodeColumnIndex = GridPane.getColumnIndex(node) == null ? 0 : GridPane.getColumnIndex(node);

            if (nodeRowIndex == rowIndex && nodeColumnIndex == columnIndex) {
                result = (Button) node;
                break;
            }
        }
        return result;
    }

    private GameServiceGrpc.GameServiceBlockingStub createClient() {
        Channel channel = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext()
                .build();
        return GameServiceGrpc.newBlockingStub(channel);
    }

    @FXML
    void onConnect(ActionEvent event) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                client = createClient();
                StartGameMessage response = client.startGame(Empty.newBuilder().build());
                myRole = response.getPlayerRole();
                gameStatus = response.getGameStatus();
                activeRole = response.getActivePlayerRole();
                if (gameStatus == GameStatus.WaitingPlayers) {
                    if (myRole == PlayerRole.Cross) {
                        Platform.runLater(() -> {
                            labelMyRole.setText("Вы играете: Х");
                            labelActiveRole.setText("Ожидание игроков");
                        });
                    } else {
                        Platform.runLater(() -> {
                            labelMyRole.setText("Вы играете: 0");
                            labelActiveRole.setText("Ожидание игроков");
                        });
                    }
                }

                //если вышли, то началась игра
                while (gameStatus == GameStatus.WaitingPlayers) {
                    gameStatus = client.getGameStatus(Empty.newBuilder().build()).getGameStatus();
                }

                Platform.runLater(() -> {
                    labelActiveRole.setText("Сейчас ходят: " + getActivePlayerSign());
                });

                while (gameStatus == GameStatus.GameStarted) {
                    //читаем данные с сервера и ждем, когда поступят обновления
                    if (activeRole != myRole) {
                        EnemyTurnMessage turnMessage = client.getEnemyTurn(grpc.EnemyRoleRequest.newBuilder().setMyRole(myRole).build());
                        if (turnMessage.getEnemyTurned()) {
                            //сходил и появились новые данные
                            activeRole = turnMessage.getActiveRole();
                            gameStatus = turnMessage.getGameStatus();
                            updateLabels();
                            updateGridByTurn(turnMessage.getRowIndex(), turnMessage.getColumnIndex(), turnMessage.getMadeMove());
                        }
                    }
                }
                gameEnd();
            }
        });
        thread.start();
        Button button = (Button) event.getSource();
        button.setDisable(true);
    }

    private void gameEnd() {

        Platform.runLater(() -> {
            if (gameStatus == GameStatus.WinZero) {
                labelGameResult.setText("Результат игры: победа ноликов");
            } else if (gameStatus == GameStatus.WinCross) {
                labelGameResult.setText("Результат игры: победа крестиков");
            } else if (gameStatus == GameStatus.Draw) {
                labelGameResult.setText("Результат игры: ничья");
            } else {
                throw new IllegalStateException("Undefined GameStatus");
            }
        });
    }

    private void updateLabels() {
        Platform.runLater(() -> {
            labelActiveRole.setText("Сейчас ходят: " + getActivePlayerSign());
        });
    }

    private void updateGridByTurn(int rowIndex, int columnIndex, PlayerRole playerRole) {
        Platform.runLater(() -> {
            Button button = findButton(rowIndex, columnIndex);
            button.setText(getPlayerSign(playerRole));
        });
    }

    private String getActivePlayerSign() {
        return activeRole == PlayerRole.Cross ? "X" : "0";
    }

    private String getPlayerSign(PlayerRole playerRole) {
        return playerRole == PlayerRole.Cross ? "X" : "0";
    }
}
