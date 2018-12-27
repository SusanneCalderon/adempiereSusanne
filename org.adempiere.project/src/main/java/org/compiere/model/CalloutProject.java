/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.model.GridTabWrapper;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;


/**
 *	Project Callouts
 *	
 *  @author Jorg Janke
 *  @version $Id: CalloutProject.java,v 1.3 2006/07/30 00:51:04 jjanke Exp $
 */
public class CalloutProject extends CalloutEngine
{
	/**
	 *	Project Planned - Price + Qty.
	 *		- called from PlannedPrice, PlannedQty
	 *		- calculates PlannedAmt (same as Trigger)
	 *  @param ctx context
	 *  @param WindowNo current Window No
	 *  @param mTab Grid Tab
	 *  @param mField Grid Field
	 *  @param value New Value
	 *  @return null or error message
	 */
	public  String planned (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive() || value == null)
			return "";

		BigDecimal PlannedQty, PlannedPrice;
		int StdPrecision = Env.getContextAsInt(ctx, WindowNo, "StdPrecision");


		//	get values
		PlannedQty = (BigDecimal)mTab.getValue("PlannedQty");
		if (PlannedQty == null)
			PlannedQty = Env.ONE;
		PlannedPrice = ((BigDecimal)mTab.getValue("PlannedPrice"));
		if (PlannedPrice == null)
			PlannedPrice = Env.ZERO;
		//
		BigDecimal PlannedAmt = PlannedQty.multiply(PlannedPrice);
		if (PlannedAmt.scale() > StdPrecision)
			PlannedAmt = PlannedAmt.setScale(StdPrecision, BigDecimal.ROUND_HALF_UP);
		//
		log.fine("PlannedQty=" + PlannedQty + " * PlannedPrice=" + PlannedPrice + " -> PlannedAmt=" + PlannedAmt + " (Precision=" + StdPrecision+ ")");
		mTab.setValue("PlannedAmt", PlannedAmt);
		return "";
	}	//	planned

	/**
	 * Fill Project Standard Phase
	 * @param ctx
	 * @param windowNo
	 * @param gridTab
	 * @param gridField
	 * @param value
	 * @return
	 */
	public String projectPhase(Properties ctx, int windowNo, GridTab gridTab, GridField gridField, Object value)
	{
		Optional<I_C_ProjectPhase> projectPhaseOptional = Optional.of(GridTabWrapper.create(gridTab, I_C_ProjectPhase.class));
		projectPhaseOptional.ifPresent(projectPhase -> {
			MProjectTypePhase projectTypePhase = (MProjectTypePhase) projectPhase.getC_Phase();
			if (projectPhase.getC_Phase_ID() > 0) {
				if (projectPhase.getName() == null || projectPhase.getName().isEmpty())
					projectPhase.setName(projectTypePhase.getName());
				if (projectPhase.getDescription() == null || projectPhase.getDescription().isEmpty())
					projectPhase.setDescription(projectTypePhase.getDescription());
				if (projectPhase.getHelp() == null || projectPhase.getHelp().isEmpty())
					projectPhase.setHelp(projectTypePhase.getHelp());
				if (projectPhase.getPriorityRule() == null || projectPhase.getPriorityRule().isEmpty())
					projectPhase.setPriorityRule(projectTypePhase.getPriorityRule());

				projectPhase.setIsMilestone(projectTypePhase.isMilestone());

				if(projectPhase.getDurationUnit() == null || projectPhase.getDurationUnit().isEmpty())
					projectPhase.setDurationUnit(projectTypePhase.getDurationUnit());
				if (projectPhase.getDurationEstimated().signum() == 0)
					projectPhase.setDurationEstimated(projectTypePhase.getDurationEstimated());

				if (projectPhase.getM_Product_ID() <=0 )
					projectPhase.setM_Product_ID(projectTypePhase.getM_Product_ID());

				if (projectPhase.getPP_Product_BOM_ID() <= 0)
					projectPhase.setPP_Product_BOM_ID(projectTypePhase.getPP_Product_BOM_ID());

				if (projectPhase.getAD_Workflow_ID() <= 0)
					projectPhase.setAD_Workflow_ID(projectTypePhase.getAD_Workflow_ID());

				if (projectPhase.getQty().signum() == 0)
					projectPhase.setQty(projectTypePhase.getStandardQty());

				projectPhase.setIsIndefinite(projectTypePhase.isIndefinite());
				projectPhase.setIsRecurrent(projectTypePhase.isRecurrent());
				projectPhase.setFrequencyType(projectTypePhase.getFrequencyType());
				projectPhase.setFrequency(projectTypePhase.getFrequency());
				projectPhase.setRunsMax(projectTypePhase.getRunsMax());
			}
		});
		return "";
	}

	/**
	 * Fill Project Task from Project Standard Task
	 * @param ctx
	 * @param windowNo
	 * @param gridTab
	 * @param gridField
	 * @param value
	 * @return
	 */
	public String projectTask(Properties ctx, int windowNo, GridTab gridTab, GridField gridField, Object value)
	{
		Optional<I_C_ProjectTask> projectTaskOptional = Optional.of(GridTabWrapper.create(gridTab, I_C_ProjectTask.class));
		projectTaskOptional.ifPresent(projectTask -> {
			MProjectTypeTask projectTypeTask = (MProjectTypeTask) projectTask.getC_Task();
			if (projectTask.getC_Task_ID() > 0) {
				if (projectTask.getName() == null || projectTask.getName().isEmpty())
					projectTask.setName(projectTypeTask.getName());
				if (projectTask.getDescription() == null || projectTask.getDescription().isEmpty())
					projectTask.setDescription(projectTypeTask.getDescription());
				if (projectTask.getHelp() == null || projectTask.getHelp().isEmpty())
					projectTask.setHelp(projectTypeTask.getHelp());
				if (projectTask.getPriorityRule() == null || projectTask.getPriorityRule().isEmpty())
					projectTask.setPriorityRule(projectTypeTask.getPriorityRule());

				projectTask.setIsMilestone(projectTypeTask.isMilestone());

				if (projectTask.getDurationUnit() == null || projectTask.getDurationUnit().isEmpty())
					projectTask.setDurationUnit(projectTypeTask.getDurationUnit());
				if (projectTask.getDurationEstimated().signum() == 0 )
					projectTask.setDurationEstimated(projectTypeTask.getDurationEstimated());
				if (projectTask.getM_Product_ID() <= 0)
					projectTask.setM_Product_ID(projectTypeTask.getM_Product_ID());

				if (projectTask.getPP_Product_BOM_ID() <= 0)
					projectTask.setPP_Product_BOM_ID(projectTypeTask.getPP_Product_BOM_ID());

				if (projectTask.getAD_Workflow_ID() <= 0)
					projectTask.setAD_Workflow_ID(projectTypeTask.getAD_Workflow_ID());

				projectTask.setIsIndefinite(projectTypeTask.isIndefinite());
				projectTask.setIsRecurrent(projectTypeTask.isRecurrent());
				projectTask.setFrequencyType(projectTypeTask.getFrequencyType());
				projectTask.setFrequency(projectTypeTask.getFrequency());
				projectTask.setRunsMax(projectTypeTask.getRunsMax());
			}
		});
		return "";
	}

	/**
	 * Complete percentage or complete flag
	 * @param ctx
	 * @param windowNo
	 * @param gridTab
	 * @param gridField
	 * @param value
	 * @return
	 */
	public String completeTask (Properties ctx, int windowNo, GridTab gridTab, GridField gridField, Object value)
	{
		Optional<I_C_ProjectTask> projectTaskOptional = Optional.of(GridTabWrapper.create(gridTab, I_C_ProjectTask.class));
		projectTaskOptional.ifPresent(projectTask -> {
			if (projectTask.getPercentageCompleted().compareTo( new BigDecimal(100)) == 0)
			{
				if (!projectTask.isComplete())
					projectTask.setIsComplete(true);
			}
			if (projectTask.isComplete())
			{
				if (projectTask.getPercentageCompleted().compareTo(new BigDecimal(100)) <= 0)
					projectTask.setPercentageCompleted(new BigDecimal(100));
			}

		});
		return null;
	}
	
	/**
	 *	Project Header - BPartner.
	 *		- M_PriceList_ID (+ Context)
	 *		- C_BPartner_Location_ID
	 *		- AD_User_ID
	 *		- POReference
	 *		- SO_Description
	 *		- PaymentRule
	 *		- C_PaymentTerm_ID
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String bPartner (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_BPartner_ID = (Integer)value;
		if (C_BPartner_ID == null || C_BPartner_ID.intValue() == 0)
			return "";
		String sql = "SELECT p.AD_Language,p.C_PaymentTerm_ID,"
			+ " COALESCE(p.M_PriceList_ID,g.M_PriceList_ID) AS M_PriceList_ID, p.PaymentRule,p.POReference,"
			+ " p.SO_Description,p.IsDiscountPrinted,"
			+ " p.InvoiceRule,p.DeliveryRule,p.FreightCostRule,DeliveryViaRule,"
			+ " p.SO_CreditLimit, p.SO_CreditLimit-p.SO_CreditUsed AS CreditAvailable,"
			+ " lship.C_BPartner_Location_ID,c.AD_User_ID,"
			+ " COALESCE(p.PO_PriceList_ID,g.PO_PriceList_ID) AS PO_PriceList_ID, p.PaymentRulePO,p.PO_PaymentTerm_ID," 
			+ " lbill.C_BPartner_Location_ID AS Bill_Location_ID, p.SOCreditStatus, "
			+ " p.SalesRep_ID "
			+ "FROM C_BPartner p"
			+ " INNER JOIN C_BP_Group g ON (p.C_BP_Group_ID=g.C_BP_Group_ID)"			
			+ " LEFT OUTER JOIN C_BPartner_Location lbill ON (p.C_BPartner_ID=lbill.C_BPartner_ID AND lbill.IsBillTo='Y' AND lbill.IsActive='Y')"
			+ " LEFT OUTER JOIN C_BPartner_Location lship ON (p.C_BPartner_ID=lship.C_BPartner_ID AND lship.IsShipTo='Y' AND lship.IsActive='Y')"
			+ " LEFT OUTER JOIN AD_User c ON (p.C_BPartner_ID=c.C_BPartner_ID) "
			+ "WHERE p.C_BPartner_ID=? AND p.IsActive='Y'";		//	#1

		boolean IsSOTrx = "Y".equals(Env.getContext(ctx, WindowNo, "IsSOTrx"));
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, C_BPartner_ID.intValue());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				// Sales Rep - If BP has a default SalesRep then default it
				Integer salesRep = rs.getInt("SalesRep_ID");
				if (IsSOTrx && salesRep != 0 )
				{
					mTab.setValue("SalesRep_ID", salesRep);
				}
				Integer salesRepPO = rs.getInt("SalesRep_ID");
				if (!IsSOTrx && salesRepPO != 0 )
				{
					mTab.setValue("C_BPartnerSR_ID", salesRepPO);
				}
				
				
				//	PriceList (indirect: IsTaxIncluded & Currency)
				Integer ii = new Integer(rs.getInt(IsSOTrx ? "M_PriceList_ID" : "PO_PriceList_ID"));
				if (!rs.wasNull())
					mTab.setValue("M_PriceList_ID", ii);
				else
				{	//	get default PriceList
					int i = Env.getContextAsInt(ctx, "#M_PriceList_ID");
					if (i != 0)
						mTab.setValue("M_PriceList_ID", new Integer(i));
				}

				// Ship-To Location
				int shipTo_ID = rs.getInt("C_BPartner_Location_ID");
				//	overwritten by InfoBP selection - works only if InfoWindow
				//	was used otherwise creates error (uses last value, may belong to different BP)
				if (C_BPartner_ID.toString().equals(Env.getContext(ctx, WindowNo, Env.TAB_INFO, "C_BPartner_ID")))
				{
					String loc = Env.getContext(ctx, WindowNo, Env.TAB_INFO, "C_BPartner_Location_ID");
					if (loc.length() > 0)
						shipTo_ID = Integer.parseInt(loc);
				}
				if (shipTo_ID == 0)
					mTab.setValue("C_BPartner_Location_ID", null);
				else
					mTab.setValue("C_BPartner_Location_ID", new Integer(shipTo_ID));

				//	Contact - overwritten by InfoBP selection
				int contID = rs.getInt("AD_User_ID");
				if (C_BPartner_ID.toString().equals(Env.getContext(ctx, WindowNo, Env.TAB_INFO, "C_BPartner_ID")))
				{
					String cont = Env.getContext(ctx, WindowNo, Env.TAB_INFO, "AD_User_ID");
					if (cont.length() > 0)
						contID = Integer.parseInt(cont);
				}
				if (contID == 0)
					mTab.setValue("AD_User_ID", null);
				else
					mTab.setValue("AD_User_ID", new Integer(contID));


				//	PO Reference
				String s = rs.getString("POReference");
				if (s != null && s.length() != 0)
					mTab.setValue("POReference", s);
				// should not be reset to null if we entered already value! VHARCQ, accepted YS makes sense that way
				
				//	SO Description
				s = rs.getString("SO_Description");
				if (s != null && s.trim().length() != 0)
					mTab.setValue("Description", s);

				//	Defaults, if not Walkin Receipt or Walkin Invoice
				mTab.setValue("PaymentRule", X_C_Order.PAYMENTRULE_OnCredit);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
			return e.getLocalizedMessage();
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return "";
	}	//	bPartner
	
	/**
	 *	Product in Phase or Task updates
	 *		- List Price
	 *		- Price Entered
	 *		- Line Amount
	 *		- Actual Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String product (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			mTab.setValue("PriceList", Env.ZERO);
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}
		
		//	No changes ?
		if( (Integer)mField.getOldValue()==M_Product_ID)
			return "";		

		Integer C_Project_ID = Env.getContextAsInt(ctx, WindowNo, "C_Project_ID");
		MProject project = MProject.getById(Env.getCtx(), C_Project_ID, null);
		
		// PriceList or PriceListVersion mandatory
 		if(project.getM_PriceList_ID()==0 && project.getM_PriceList_Version_ID()==0) {
			String info = Msg.parseTranslation(ctx, "@PriceListNotFound@" + ", " + "@PriceListVersionNotFound@");
			mTab.fireDataStatusEEvent ("Error", info, false);
		}
 		
 		// Find out MProductPrice
		int M_PriceList_Version_ID =0;
		if (project.getM_PriceList_Version_ID()==0) {
			String sql = "SELECT plv.M_PriceList_Version_ID "
				+ "FROM M_PriceList_Version plv "
				+ "WHERE plv.M_PriceList_ID=? "						//	1
				+ " AND plv.ValidFrom <= ? "
				+ " AND plv.isActive = 'Y' "
				+ "ORDER BY plv.ValidFrom DESC";
			//	Use newest price list - may not be future
			
			Timestamp dateStartSchedule = project.getDateStartSchedule();
			M_PriceList_Version_ID = DB.getSQLValueEx(null, sql, project.getM_PriceList_ID(), dateStartSchedule);
			//priceListVersion = new MPriceListVersion(Env.getCtx(), M_PriceList_Version_ID, null);
			
		}
		else {
			M_PriceList_Version_ID = project.getM_PriceList_Version_ID();
			//priceListVersion = new MPriceListVersion(Env.getCtx(), project.getM_PriceList_Version_ID(), null);
		}
		
		//MProduct product = MProduct.get (Env.getCtx(), M_Product_ID.intValue());
		MProductPrice pp = MProductPrice.get(Env.getCtx(), M_PriceList_Version_ID, M_Product_ID, null);
		if (pp==null) {
			String info = Msg.parseTranslation(ctx, "@ProductNotOnPriceList@");
			mTab.fireDataStatusEEvent ("Error", info, false);
		}
		else  {
			BigDecimal listPrice = pp.getPriceList();
			BigDecimal qty = (BigDecimal) mTab.getValue("Qty");
			mTab.setValue("PriceList", listPrice);
			mTab.setValue("PriceEntered", listPrice);
			mTab.setValue("ActualAmt", qty.multiply(listPrice));
			mTab.setValue("LineNetAmt", qty.multiply(listPrice));
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
		}
		
		
		return "";
	}	//	product
	
	/**
	 *	Quantity in Phase or Task updates
	 *		- Actual Amount
	 *		- Line Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String qty (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";

		// Product empty?
		Integer M_Product_ID = (Integer)mTab.getValue("M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			mTab.setValue("PriceList", Env.ZERO);
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}
		
		BigDecimal qty = (BigDecimal)value;
		BigDecimal listPrice = (BigDecimal) mTab.getValue("PriceList");
		BigDecimal priceEntered = (BigDecimal) mTab.getValue("PriceEntered");
		BigDecimal lineAmt = qty.multiply(listPrice);
		BigDecimal actualAmt = qty.multiply(priceEntered);
		mTab.setValue("LineNetAmt", lineAmt);
		mTab.setValue("ActualAmt", actualAmt);
		mTab.setValue("MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
		if (lineAmt.compareTo(Env.ZERO)!=0)
			mTab.setValue("Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
					.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
		else
			mTab.setValue("Margin", Env.ZERO);

		return "";
	}	//	qty
	
	/**
	 *	Price Entered in Phase or Task updates
	 *		- Actual Amount
	 *		- Line Amount
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String priceEntered (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";
		
		// Product empty?
		Integer M_Product_ID = (Integer)mTab.getValue("M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			mTab.setValue("PriceList", Env.ZERO);
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}

		BigDecimal priceEntered =  (BigDecimal)value;
		if (priceEntered.compareTo(Env.ZERO)==0) {
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("MarginAmt", ((BigDecimal) mTab.getValue("LineNetAmt")).negate());
			mTab.setValue("Margin", Env.ONEHUNDRED.negate());
		}			
		else  {
			BigDecimal qty = (BigDecimal) mTab.getValue("Qty");
			BigDecimal listPrice = (BigDecimal) mTab.getValue("PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			BigDecimal actualAmt = qty.multiply(priceEntered);
			mTab.setValue("LineNetAmt", lineAmt);
			mTab.setValue("ActualAmt", actualAmt);
			mTab.setValue("MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
			if (lineAmt.compareTo(Env.ZERO)!=0)
				mTab.setValue("Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
		}	
		return "";
	}	//	priceEntered
	
	/**
	 *	Actual Amount in Phase or Task updates
	 *		- Price Entered
	 *		- Margin Amount
	 *		- Margin (%)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String actualAmt (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";

		// Product empty?
		Integer M_Product_ID = (Integer)mTab.getValue("M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			mTab.setValue("PriceList", Env.ZERO);
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}

		BigDecimal actualAmt = (BigDecimal)value;
		if (actualAmt.compareTo(Env.ZERO)==0) {
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("MarginAmt", ((BigDecimal) mTab.getValue("LineNetAmt")).negate());
			mTab.setValue("Margin", Env.ONEHUNDRED.negate());
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) mTab.getValue("Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
		}
		else {
			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			mTab.setValue("PriceEntered", priceEntered);
			BigDecimal listPrice = (BigDecimal) mTab.getValue("PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			mTab.setValue("LineNetAmt", lineAmt);
			
			mTab.setValue("MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
			if (lineAmt.compareTo(Env.ZERO)!=0)
				mTab.setValue("Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}
		return "";
	}	//	actualAmt
	
	/**
	 *	Margin (%) in Phase or Task updates
	 *		- Actual Amount
	 *		- Price Entered
	 *		- Margin Amount
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String marginPercentage (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";

		// Product empty?
		Integer M_Product_ID = (Integer)mTab.getValue("M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			mTab.setValue("PriceList", Env.ZERO);
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}

		BigDecimal margin = (BigDecimal)value;
		if (margin.compareTo(Env.ONEHUNDRED.negate())==-1) {
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("MarginAmt", ((BigDecimal) mTab.getValue("LineNetAmt")).negate());
			mTab.setValue("Margin", Env.ONEHUNDRED.negate());
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) mTab.getValue("Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
		}
		else {
			BigDecimal listPrice = (BigDecimal) mTab.getValue("PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			mTab.setValue("LineNetAmt", lineAmt);
			BigDecimal actualAmt = margin.divide(Env.ONEHUNDRED, 6, BigDecimal.ROUND_HALF_UP).add(Env.ONE).multiply(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
			mTab.setValue("ActualAmt", actualAmt);
			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			mTab.setValue("PriceEntered", priceEntered);
			mTab.setValue("MarginAmt", actualAmt.subtract(lineAmt).setScale(2, BigDecimal.ROUND_HALF_UP));
		}
		return "";
	}	//	marginPercentage
	
	/**
	 *	Margin Amount in Phase or Task updates
	 *		- Actual Amount
	 *		- Price Entered
	 *		- Margin %
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String marginAmt (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";

		// Product empty?
		Integer M_Product_ID = (Integer)mTab.getValue("M_Product_ID");
		if (M_Product_ID == null || M_Product_ID.intValue() == 0) {
			mTab.setValue("PriceList", Env.ZERO);
			mTab.setValue("PriceEntered", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}

		BigDecimal marginAmt = (BigDecimal)value;
		if (marginAmt.compareTo(Env.ZERO)==0) {
			mTab.setValue("PriceEntered", (BigDecimal) mTab.getValue("PriceList"));
			mTab.setValue("ActualAmt", (BigDecimal) mTab.getValue("LineNetAmt"));
			mTab.setValue("Margin", Env.ZERO);
			return "";		
		}		
		
		BigDecimal qty = (BigDecimal) mTab.getValue("Qty");
		if (qty.compareTo(Env.ZERO)==0) {
			mTab.setValue("LineNetAmt", Env.ZERO);
			mTab.setValue("ActualAmt", Env.ZERO);
			mTab.setValue("MarginAmt", Env.ZERO);
			mTab.setValue("Margin", Env.ZERO);
		}
		else {
			BigDecimal listPrice = (BigDecimal) mTab.getValue("PriceList");
			BigDecimal lineAmt = qty.multiply(listPrice);
			mTab.setValue("LineNetAmt", lineAmt);

			BigDecimal actualAmt = marginAmt.add(lineAmt);
			mTab.setValue("ActualAmt", actualAmt);

			BigDecimal priceEntered =  actualAmt.divide(qty, 6, BigDecimal.ROUND_HALF_UP);
			mTab.setValue("PriceEntered", priceEntered);
			
			if (lineAmt.compareTo(Env.ZERO)!=0)
				mTab.setValue("Margin",actualAmt.divide(lineAmt, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE)
						.multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}
		return "";
	}	//	marginAmt
}	//	CalloutProject
