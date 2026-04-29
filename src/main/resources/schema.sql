
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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        academic_year_id integer not null,
        class_id integer not null,
        exam_routine_id integer not null,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        marks_obtained float(53),
        shift_id integer not null,
        student_group_id integer,
        created_at datetime(6),
        enrollment_id bigint not null,
        section_id bigint,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint uq_enrollment_exam_session unique (enrollment_id, exam_session_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        academic_year_id integer not null,
        class_id integer not null,
        exam_routine_id integer not null,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        marks_obtained float(53),
        shift_id integer not null,
        student_group_id integer,
        created_at datetime(6),
        enrollment_id bigint not null,
        section_id bigint,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint uq_enrollment_exam_session unique (enrollment_id, exam_session_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        academic_year_id integer not null,
        class_id integer not null,
        exam_routine_id integer not null,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        marks_obtained float(53),
        shift_id integer not null,
        student_group_id integer,
        created_at datetime(6),
        enrollment_id bigint not null,
        section_id bigint,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint uq_enrollment_exam_session unique (enrollment_id, exam_session_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        academic_year_id integer not null,
        class_id integer not null,
        exam_routine_id integer not null,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        marks_obtained float(53),
        shift_id integer not null,
        student_group_id integer,
        created_at datetime(6),
        enrollment_id bigint not null,
        section_id bigint,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint uq_enrollment_exam_session unique (enrollment_id, exam_session_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        pass_marks integer,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        pass_marks integer,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        pass_marks integer,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        pass_marks integer,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);

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

    create table class_subject_group (
        class_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        student_group_id integer,
        subject_id integer not null,
        primary key (id)
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

    create table exam_component (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        deleted_at datetime(6),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_routine (
        academic_year_id integer not null,
        exam_type_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        created_at datetime(6),
        last_modified_at datetime(6),
        published_at datetime(6),
        title varchar(255) not null,
        status enum ('DRAFT','PUBLISHED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table exam_seat_allocation (
        end_roll integer,
        exam_session_id integer not null,
        gender_section_id integer,
        id integer not null auto_increment,
        room_id integer,
        start_roll integer,
        last_modified_at datetime(6),
        section_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table exam_session (
        class_id integer not null,
        date date not null,
        end_time time(0) not null,
        exam_routine_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        start_time time(0) not null,
        subject_id integer not null,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table exam_type (
        id integer not null auto_increment,
        is_active bit,
        order_index integer,
        name varchar(255) not null,
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

    create table grades (
        gpa_value float(53) not null,
        max_mark float(53) not null,
        min_mark float(53) not null,
        grading_policy_id bigint not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table grading_policies (
        is_active bit not null,
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure (
        class_id integer not null,
        exam_type_id integer not null,
        group_id integer,
        id integer not null auto_increment,
        is_active bit,
        pass_marks integer,
        subject_id integer not null,
        total_marks integer not null,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table marking_structure_component (
        exam_component_id integer not null,
        id integer not null auto_increment,
        is_active bit,
        marking_structure_id integer not null,
        max_marks integer not null,
        pass_marks integer,
        created_at datetime(6),
        deleted_at datetime(6),
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table room (
        capacity integer,
        id integer not null auto_increment,
        is_active bit,
        name varchar(255) not null,
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

    create table student_mark (
        exam_component_id integer not null,
        exam_session_id integer not null,
        marks_obtained decimal(38,2),
        created_at datetime(6),
        deleted_at datetime(6),
        enrollment_id bigint not null,
        id bigint not null auto_increment,
        last_modified_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table student_status (
        id integer not null auto_increment,
        status_name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        id integer not null auto_increment,
        is_active bit,
        code varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table class_subject_group 
       add constraint UKn4lb6bcsf6nvcyn5vikragboc unique (class_id, subject_id, student_group_id);

    alter table exam_component 
       add constraint UKqublmvbbo0vd988qtnxvp347a unique (name);

    alter table exam_type 
       add constraint UKgksn550gjdnp9pugu1yinnd1i unique (name);

    alter table gender_section 
       add constraint UKcj8mhj9n01pglh47fg6irbu1y unique (gender_name);

    alter table grading_policies 
       add constraint UKnakdyh2jf5s64xxagpt9cvmks unique (name);

    alter table marking_structure 
       add constraint UK90qngqow82qwix0hxk74j1ocb unique (exam_type_id, class_id, subject_id, group_id);

    alter table marking_structure_component 
       add constraint UKhyhppx8daafssq76a8ku7cad4 unique (marking_structure_id, exam_component_id);

    alter table room 
       add constraint UK4l8mm4fqoos6fcbx76rvqxer unique (name);

    alter table student_mark 
       add constraint UKpdsnw0p9y931yung1mxk4wk70 unique (enrollment_id, exam_session_id, exam_component_id);

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

    alter table class_subject_group 
       add constraint FKjcgrx4s3hv8u4q0atc2xmmedh 
       foreign key (class_id) 
       references class (id);

    alter table class_subject_group 
       add constraint FKhdqil57mcqygnhrhhavue7b4q 
       foreign key (subject_id) 
       references subject (id);

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

    alter table exam_routine 
       add constraint FKlvax7f1kipdx1vli7yeuys1b7 
       foreign key (academic_year_id) 
       references academic_year (id);

    alter table exam_routine 
       add constraint FK1hxn6fbkbra2g2f28itr2mabf 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table exam_seat_allocation 
       add constraint FKffaipfrmoyyf6sk4q7dtpmcrp 
       foreign key (exam_session_id) 
       references exam_session (id);

    alter table exam_session 
       add constraint FK60jf5766yl384ouraisb66h6k 
       foreign key (class_id) 
       references class (id);

    alter table exam_session 
       add constraint FK5rj40x5n42ij0jdro338e2wm8 
       foreign key (exam_routine_id) 
       references exam_routine (id);

    alter table exam_session 
       add constraint FKajgts42053ciol79at2q9lasj 
       foreign key (subject_id) 
       references subject (id);

    alter table grades 
       add constraint FKoy5giccdn5b0w5v2r8ys8itf7 
       foreign key (grading_policy_id) 
       references grading_policies (id);

    alter table marking_structure 
       add constraint FKke6l0aus7cteomph5cc4k6xeh 
       foreign key (class_id) 
       references class (id);

    alter table marking_structure 
       add constraint FKt7onp54y7mnjg5nm4banxccfe 
       foreign key (exam_type_id) 
       references exam_type (id);

    alter table marking_structure 
       add constraint FKgextkxe2hbm0wlcubd6i9se8y 
       foreign key (subject_id) 
       references subject (id);

    alter table marking_structure_component 
       add constraint FKnlvke2i3jl7sv3841qyse1we8 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table marking_structure_component 
       add constraint FK98m8jgf91e7g88q5m921ge2ck 
       foreign key (marking_structure_id) 
       references marking_structure (id);

    alter table section 
       add constraint FK8l8i27bhro0d5mjvx6xgw1w4m 
       foreign key (class_id) 
       references class (id);

    alter table section 
       add constraint FK4fsqdy9d5nh1isbjwm7kegsiq 
       foreign key (gender_id) 
       references gender_section (id);

    alter table student_mark 
       add constraint FKssun8812i612d531lvt97wdx1 
       foreign key (exam_component_id) 
       references exam_component (id);

    alter table student_mark 
       add constraint FKleqww1gyi9mwxdq99nscry884 
       foreign key (exam_session_id) 
       references exam_session (id);
