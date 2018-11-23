/*
 *This file is part of Adempiere ERP Bazaar
 *http://www.adempiere.org
 *Copyright (C) 2006-2008 victor.perez@e-evolution.com, e-Evolution
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.of 
 * Title:	Return Standard Price of Product/BOM
 * Description:
 */
CREATE OR REPLACE FUNCTION Bompricestd
(
	Product_ID 		numeric,
	PriceList_Version_ID	numeric
)
RETURNS numeric
AS
$BODY$
BEGIN
	RETURN Bompricestd(Product_ID, PriceList_Version_ID, 0);
END;
$BODY$
  LANGUAGE 'plpgsql' ;
