{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  packages = with pkgs; [
    jdk21
    docker-compose
    openssl
  ];

  JAVA_HOME = "${pkgs.jdk21}";
}
