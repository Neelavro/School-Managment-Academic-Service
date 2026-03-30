
    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    create table academic_year (
        id integer not null auto_increment,
        is_active bit,
        year_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        shift_id integer,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table class_student_group (
        class_id integer not null,
        student_group_id integer not null,
        primary key (class_id, student_group_id)
    ) engine=InnoDB;

    create table enrollment (
        academic_year_id integer,
        class_id integer,
        class_roll integer,
        gender_section_id integer,
        is_active bit,
        shift_id integer,
        student_group_id integer,
        id bigint not null auto_increment,
        section_id bigint,
        student_system_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table gender (
        id integer not null auto_increment,
        is_active bit,
        gender varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table gender_section (
        id integer not null auto_increment,
        gender_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table section (
        class_id integer not null,
        gender_id integer,
        is_active bit,
        id bigint not null auto_increment,
        section_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shift (
        id integer not null auto_increment,
        is_active bit,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_group (
        id integer not null auto_increment,
        is_active bit,
        group_name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table student_status 
       add constraint UKhp4wxyk7deyc2u7d67eqng1qo unique (status_name);

    alter table class 
       add constraint FKsv8g4iaxs8f691iu6ka8aylmu 
       foreign key (shift_id) 
       references shift (id);

    alter table class_student_group 
       add constraint FK1r6r51b0cir9uh2856bt4bb57 
       foreign key (student_group_id) 
       references student_group (id);

    alter table class_student_group 
       add constraint FKqrdwnejesthguoxp9a7vevvwl 
       foreign key (class_id) 
       references class (id);

    alter table enrollment 
       add constraint FKi2c80igmdt44huydp7gv09khn 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table enrollment 
       add constraint FKs2r5l1eva5jl9o3xqeyst6yvc 
       foreign key (gender_section_id) 
       references gender_section (id);

    alter table enrollment 
       add constraint FKamt2st6hpbqje7p6jwqd6cm5e 
       foreign key (section_id) 
       references section (id);

    alter table enrollment 
       add constraint FKsup6nptkpo9ab0d4k3o54bi5k 
       foreign key (shift_id) 
       references shift (id);

    alter table enrollment 
       add constraint FK5v613v80qptj9japxdb9g9eti 
       foreign key (class_id) 
       references class (id);

    alter table enrollment 
       add constraint FKorfuifbyh6hdkq7rovr5xuohq 
       foreign key (student_group_id) 
       references student_group (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);
