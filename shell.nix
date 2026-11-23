{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  packages = with pkgs; [
    jdk21
    docker-compose
  ];

  JAVA_HOME = "${pkgs.jdk21}";
}
