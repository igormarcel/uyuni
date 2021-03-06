-- Copyright (c) 2012 Novell
--
-- This software is licensed to you under the GNU General Public License,
-- version 2 (GPLv2). There is NO WARRANTY for this software, express or
-- implied, including the implied warranties of MERCHANTABILITY or FITNESS
-- FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
-- along with this software; if not, see
-- http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
--

create or replace function insert_md_keyword(label_in in varchar2)
return number
is
    pragma autonomous_transaction;
    md_keyword_id  number;
begin
    insert into suseMdKeyword (id, label)
    values (suse_mdkeyword_id_seq.nextval, label_in) returning id into md_keyword_id;
    commit;
    return md_keyword_id;
end;
/
show errors
