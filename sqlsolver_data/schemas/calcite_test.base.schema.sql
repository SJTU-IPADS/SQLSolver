CREATE TABLE `account`
(
    `acctno`  int         not null,
    `type`    varchar(20) not null,
    `balance` int         not null
);

CREATE TABLE `bonus`
(
    `ename` varchar(20) not null,
    `job`   varchar(10) not null,
    `sal`   int         not null,
    `comm`  int         not null
);

CREATE TABLE `dept`
(
    `deptno` int primary key not null,
    `name`   varchar(10)     not null
);

CREATE TABLE `emp`
(
    `empno`    int primary key not null,
    `ename`    varchar(20)     not null,
    `job`      varchar(10)     not null,
    `mgr`      int             null,
    `hiredate` int             not null,
    `sal`      int             not null,
    `comm`     int             not null,
    `deptno`   int not null references dept (deptno),
    `slacker`  tinyint
);

CREATE TABLE `T`
(
    `K0`    varchar(20) not null,
    `C1`    varchar(20),
    `F1_A0` int         not null,
    `F2_A0` tinyint     not null,
    `F0_C0` int         not null,
    `F1_C0` int,
    `F0_C1` int         not null,
    `F1_C2` int         not null,
    `F2_C3` int         not null
);

CREATE TABLE ANON
(
    c INT
);